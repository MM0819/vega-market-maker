package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.exception.TradingException;
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

    public UpdateQuotesTask(@Value("${vega.market.id}") String marketId,
                            ReferencePriceStore referencePriceStore,
                            AppConfigStore appConfigStore,
                            OrderStore orderStore,
                            VegaApiClient vegaApiClient,
                            MarketService marketService,
                            AccountService accountService,
                            PositionService positionService,
                            PricingUtils pricingUtils) {
        this.appConfigStore = appConfigStore;
        this.marketId = marketId;
        this.referencePriceStore = referencePriceStore;
        this.orderStore = orderStore;
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.pricingUtils = pricingUtils;
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
        Market market = marketService.getById(marketId);
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        BigDecimal exposure = positionService.getExposure(marketId);
        AppConfig config = appConfigStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND));
        BigDecimal referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND)).getMidPrice();
        BigDecimal bidPoolSize = balance.multiply(BigDecimal.valueOf(0.5));
        BigDecimal askPoolSize = bidPoolSize.divide(referencePrice, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        BigDecimal openVolumeRatio = exposure.divide(askPoolSize, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        double scalingFactor = pricingUtils.getScalingFactor(openVolumeRatio.doubleValue());
        List<DistributionStep> askDistribution = pricingUtils.getAskDistribution(
                exposure.doubleValue() < 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getAskQuoteRange(), config.getOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getBidDistribution(
                exposure.doubleValue() > 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getBidQuoteRange(), config.getOrderCount());
        List<Order> bids = bidDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getBidSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.NEW)
                        .setSide(MarketSide.BUY)
                        .setType(OrderType.LIMIT)
        ).collect(Collectors.toList());
        List<Order> asks = askDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getAskSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.NEW)
                        .setSide(MarketSide.SELL)
                        .setType(OrderType.LIMIT)
        ).collect(Collectors.toList());
        List<Order> currentOrders = orderStore.getItems();
        List<Order> currentBids = currentOrders.stream()
                .filter(o -> o.getSide().equals(MarketSide.BUY))
                .collect(Collectors.toList());
        List<Order> currentAsks = currentOrders.stream()
                .filter(o -> o.getSide().equals(MarketSide.SELL))
                .collect(Collectors.toList());
        bids.sort(Comparator.comparing(Order::getPrice));
        currentBids.sort(Comparator.comparing(Order::getPrice));
        asks.sort(Comparator.comparing(Order::getPrice).reversed());
        currentAsks.sort(Comparator.comparing(Order::getPrice).reversed());
        int askCount = Math.max(asks.size(), currentAsks.size());
        int bidCount = Math.max(bids.size(), currentBids.size());
        updateSideOfBook(bids, currentBids, bidCount);
        updateSideOfBook(asks, currentAsks, askCount);
    }

    /**
     * Update one side of the order book one order at a time, alternating between submit and cancel
     *
     * @param newOrders {@link List<Order>}
     * @param currentOrders {@link List<Order>}
     * @param count total iterations
     */
    private void updateSideOfBook(
            List<Order> newOrders,
            List<Order> currentOrders,
            int count
    ) {
        for(int i=0; i<count; i++) {
            if(i < newOrders.size()) {
                vegaApiClient.submitOrder(newOrders.get(i));
            }
            if(i < currentOrders.size()) {
                vegaApiClient.cancelOrder(currentOrders.get(i).getId());
            }
        }
    }
}