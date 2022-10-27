package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    private final String updateQuotesCronExpression;

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
                            @Value("${update.quotes.cron.expression}") String updateQuotesCronExpression) {
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
        this.updateQuotesCronExpression = updateQuotesCronExpression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCronExpression() {
        return updateQuotesCronExpression;
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
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        BigDecimal midPrice = referencePrice.getMidPrice();
        BigDecimal bidPoolSize = balance.multiply(BigDecimal.valueOf(0.5));
        BigDecimal askPoolSize = bidPoolSize.divide(midPrice, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        double openVolumeRatio = exposure.abs().doubleValue() / askPoolSize.doubleValue();
        log.info("\n\nReference price = {}\nExposure = {}\nBid pool size = {}\nAsk pool size = {}\n",
                referencePrice, exposure, bidPoolSize, askPoolSize);
        BigDecimal bidVolume = askPoolSize.multiply(BigDecimal.valueOf(config.getCommitmentBalanceRatio()));
        BigDecimal askVolume = askPoolSize.multiply(BigDecimal.valueOf(config.getCommitmentBalanceRatio()));
        double bidQuoteRange = config.getBidQuoteRange();
        double askQuoteRange = config.getAskQuoteRange();
        if(exposure.doubleValue() > 0) {
            bidVolume = bidVolume.multiply(BigDecimal.valueOf(1 - openVolumeRatio));
            bidQuoteRange = bidQuoteRange * (1 + openVolumeRatio);
        } else if(exposure.doubleValue() < 0) {
            askVolume = askVolume.multiply(BigDecimal.valueOf(1 - openVolumeRatio));
            askQuoteRange = askQuoteRange * (1 + openVolumeRatio);
        }
        List<DistributionStep> askDistribution = pricingUtils.getDistribution(
                referencePrice.getAskPrice().doubleValue(), askVolume.doubleValue(), askQuoteRange, MarketSide.SELL);
        List<DistributionStep> bidDistribution = pricingUtils.getDistribution(
                referencePrice.getBidPrice().doubleValue(), bidVolume.doubleValue(), bidQuoteRange, MarketSide.BUY);
        if(bidDistribution.size() == 0) {
            log.warn("Bid distribution was empty !!");
            return;
        }
        if(askDistribution.size() == 0) {
            log.warn("Ask distribution was empty !!");
            return;
        }
        List<Order> bids = bidDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getBidSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.BUY)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(TimeInForce.GTC)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).sorted(Comparator.comparing(Order::getPrice).reversed()).toList();
        List<Order> asks = askDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * config.getAskSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.SELL)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(TimeInForce.GTC)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).sorted(Comparator.comparing(Order::getPrice)).toList();
        Order bestBid = bids.get(0);
        Order bestAsk = asks.get(0);
        double targetSpread = config.getMinSpread() +
                (openVolumeRatio * (config.getMaxSpread() - config.getMinSpread()));
        double currentSpread = bestAsk.getPrice().doubleValue() - bestBid.getPrice().doubleValue();
        if(currentSpread < targetSpread) {
            double spreadDiff = (targetSpread - currentSpread) / 2.0;
            bids.forEach(b -> b.setPrice(b.getPrice().subtract(BigDecimal.valueOf(spreadDiff))));
            asks.forEach(a -> a.setPrice(a.getPrice().add(BigDecimal.valueOf(spreadDiff))));
        }
        log.info("Bid price = {}; Ask price = {}", bestBid.getPrice(), bestAsk.getPrice());
        List<Order> submissions = new ArrayList<>();
        submissions.addAll(bids);
        submissions.addAll(asks);
        List<Order> currentOrders = orderStore.getItems().stream().filter(o -> !o.getIsPeggedOrder()).toList();
        List<String> cancellations = currentOrders.stream().map(Order::getId).toList();
        int maxBatchSize = 100; // TODO this needs to come from network parameters
        // TODO - only update the order book if the mid price has moved substantially, or if there's a significant
        //  change to the LP's open volume
        if((cancellations.size() + submissions.size()) <= maxBatchSize) {
            vegaApiClient.submitBulkInstruction(cancellations, submissions, market, partyId, 0);
        } else {
            List<List<String>> cancellationBatches = ListUtils.partition(cancellations, maxBatchSize);
            List<List<Order>> submissionBatches = ListUtils.partition(submissions, maxBatchSize);
            for (List<Order> batch : submissionBatches) {
                vegaApiClient.submitBulkInstruction(Collections.emptyList(), batch, market, partyId, 0);
            }
            for (List<String> batch : cancellationBatches) {
                vegaApiClient.submitBulkInstruction(batch, Collections.emptyList(), market, partyId, 0);
            }
        }
        log.info("Quotes successfully updated!");
    }
}