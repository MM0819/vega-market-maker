package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpdateQuotesTask extends TradingTask {

    private final AppConfigStore appConfigStore;
    private final ReferencePriceStore referencePriceStore;
    private final OrderStore orderStore;
    private final VegaApiClient vegaApiClient;
    private final String marketId;
    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final PricingUtils pricingUtils;
    private final String partyId;
    private final Integer threadPoolSize;

    public UpdateQuotesTask(@Value("${vega.market.id}") String marketId,
                            @Value("${update.quotes.enabled}") Boolean taskEnabled,
                            @Value("${vega.party.id}") String partyId,
                            ReferencePriceStore referencePriceStore,
                            AppConfigStore appConfigStore,
                            OrderStore orderStore,
                            VegaApiClient vegaApiClient,
                            MarketService marketService,
                            AccountService accountService,
                            PositionService positionService,
                            PricingUtils pricingUtils,
                            DataInitializer dataInitializer,
                            WebSocketInitializer webSocketInitializer,
                            @Value("${thread.pool.size}") Integer threadPoolSize) {
        super(dataInitializer, webSocketInitializer, taskEnabled);
        this.appConfigStore = appConfigStore;
        this.marketId = marketId;
        this.referencePriceStore = referencePriceStore;
        this.orderStore = orderStore;
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.pricingUtils = pricingUtils;
        this.partyId = partyId;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCronExpression() {
        return "*/15 * * * * *";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if(!isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        if(!taskEnabled) {
            log.debug("Cannot execute {} because it is disabled", getClass().getSimpleName());
            return;
        }
        log.info("Updating quotes...");
        Market market = marketService.getById(marketId);
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance.doubleValue() == 0) {
            log.info("Cannot update quotes because balance = {}", balance);
            return;
        }
        BigDecimal exposure = positionService.getExposure(marketId);
        AppConfig config = appConfigStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND));
        BigDecimal referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND)).getMidPrice();
        BigDecimal bidPoolSize = balance.multiply(BigDecimal.valueOf(0.5));
        BigDecimal askPoolSize = bidPoolSize.divide(referencePrice, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        BigDecimal openVolumeRatio = exposure.abs().divide(askPoolSize,
                market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        double scalingFactor = pricingUtils.getScalingFactor(openVolumeRatio.doubleValue());
        log.info("\n\nReference price = {}\nExposure = {}\nBid pool size = {}\nAsk pool size = {}\n",
                referencePrice, exposure, bidPoolSize, askPoolSize);
        List<DistributionStep> askDistribution = pricingUtils.getAskDistribution(
                exposure.doubleValue() < 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getAskQuoteRange(), config.getOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getBidDistribution(
                exposure.doubleValue() > 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getBidQuoteRange(), config.getOrderCount());
        if(bidDistribution.size() == 0) {
            log.warn("Bid distribution was empty !!");
            return;
        }
        if(askDistribution.size() == 0) {
            log.warn("Ask distribution was empty !!");
            return;
        }
        bidDistribution.get(0).setPrice(bidDistribution.get(0).getPrice() * (1 - config.getSpread()));
        askDistribution.get(0).setPrice(askDistribution.get(0).getPrice() * (1 + config.getSpread()));
        TimeInForce tif = market.getTradingMode().equals(MarketTradingMode.CONTINUOUS) ?
                TimeInForce.GTC : TimeInForce.GFA;
        List<Order> bids = bidDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getBidSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.BUY)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(tif)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).collect(Collectors.toList());
        List<Order> asks = askDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getAskSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.SELL)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(tif)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).collect(Collectors.toList());
        List<Order> currentOrders = orderStore.getItems();
        List<Order> currentBids = currentOrders.stream()
                .filter(o -> o.getSide().equals(MarketSide.BUY) && o.getStatus().equals(OrderStatus.ACTIVE))
                .collect(Collectors.toList());
        List<Order> currentAsks = currentOrders.stream()
                .filter(o -> o.getSide().equals(MarketSide.SELL) && o.getStatus().equals(OrderStatus.ACTIVE))
                .collect(Collectors.toList());
        bids.sort(Comparator.comparing(Order::getPrice).reversed());
        currentBids.sort(Comparator.comparing(Order::getPrice).reversed());
        asks.sort(Comparator.comparing(Order::getPrice));
        currentAsks.sort(Comparator.comparing(Order::getPrice));
        updateQuotes(bids, asks, currentBids, currentAsks);
        log.info("Quotes successfully updated!");
    }

    /**
     * Reprice the quotes
     *
     * @param newBids {@link List<Order>}
     * @param newAsks {@link List<Order>}
     * @param currentBids {@link List<Order>}
     * @param currentAsks {@link List<Order>}
     */
    private void updateQuotes(
            List<Order> newBids,
            List<Order> newAsks,
            List<Order> currentBids,
            List<Order> currentAsks
    ) {
        int count = Math.max(
                Math.max(newBids.size(), newAsks.size()),
                Math.max(currentBids.size(), currentAsks.size())
        );
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        for(int i=0; i<count; i++) {
            int x = i;
            executorService.submit(() -> updateSideOfBook(currentAsks, newAsks, x));
            executorService.submit(() -> updateSideOfBook(currentBids, newBids, x));
        }
    }

    /**
     * Update one side of the order book (for given step)
     *
     * @param currentOrders {@link List<Order>}
     * @param newOrders {@link List<Order>}
     * @param i the current step
     */
    private void updateSideOfBook(
            final List<Order> currentOrders,
            final List<Order> newOrders,
            final int i
    ) {
        if (currentOrders.size() > i && newOrders.size() > i) {
            Order currentOrder = currentOrders.get(i);
            Order newOrder = newOrders.get(i);
            BigDecimal sizeDelta = newOrder.getSize().subtract(currentOrder.getSize());
            BigDecimal price = newOrder.getPrice();
            Market market = newOrder.getMarket();
            String partyId = newOrder.getPartyId();
            vegaApiClient.amendOrder(currentOrder.getId(), sizeDelta, price, market, partyId);
        } else if (currentOrders.size() <= i && newOrders.size() > i) {
            Order newOrder = newOrders.get(i);
            vegaApiClient.submitOrder(newOrder, partyId);
        } else if (currentOrders.size() > i) {
            Order currentOrder = currentOrders.get(i);
            vegaApiClient.cancelOrder(currentOrder.getId(), partyId);
        }
    }
}