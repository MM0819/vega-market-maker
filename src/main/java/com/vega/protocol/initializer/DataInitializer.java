package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.PositionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final OrderStore orderStore;
    private final MarketStore marketStore;
    private final PositionStore positionStore;
    private final AppConfigStore appConfigStore;
    private final VegaApiClient vegaApiClient;
    private final String partyId;
    private final Double fee;
    private final Double spread;
    private final Integer orderCount;
    private final Double bidSizeFactor;
    private final Double askSizeFactor;
    private final Double bidQuoteRange;
    private final Double askQuoteRange;
    private final Double pricingStepSize;

    public DataInitializer(OrderStore orderStore,
                           MarketStore marketStore,
                           PositionStore positionStore,
                           AppConfigStore appConfigStore,
                           VegaApiClient vegaApiClient,
                           @Value("${vega.party.id}") String partyId,
                           @Value("${fee}") Double fee,
                           @Value("${spread}") Double spread,
                           @Value("${order.count}") Integer orderCount,
                           @Value("${bid.size.factor}") Double bidSizeFactor,
                           @Value("${ask.size.factor}") Double askSizeFactor,
                           @Value("${bid.quote.range}") Double bidQuoteRange,
                           @Value("${ask.quote.range}") Double askQuoteRange,
                           @Value("${pricing.step.size}") Double pricingStepSize) {
        this.orderStore = orderStore;
        this.marketStore = marketStore;
        this.positionStore = positionStore;
        this.appConfigStore = appConfigStore;
        this.vegaApiClient = vegaApiClient;
        this.partyId = partyId;
        this.fee = fee;
        this.spread = spread;
        this.orderCount = orderCount;
        this.bidSizeFactor = bidSizeFactor;
        this.askSizeFactor = askSizeFactor;
        this.bidQuoteRange = bidQuoteRange;
        this.askQuoteRange = askQuoteRange;
        this.pricingStepSize = pricingStepSize;
    }

    /**
     * Initialize data
     */
    public void initialize() {
        AppConfig config = new AppConfig()
                .setFee(fee)
                .setSpread(spread)
                .setOrderCount(orderCount)
                .setBidSizeFactor(bidSizeFactor)
                .setAskSizeFactor(askSizeFactor)
                .setBidQuoteRange(bidQuoteRange)
                .setAskQuoteRange(askQuoteRange)
                .setPricingStepSize(pricingStepSize);
        appConfigStore.update(config);
        // TODO - get accounts
        vegaApiClient.getMarkets().forEach(marketStore::add);
        vegaApiClient.getOpenOrders(partyId).forEach(orderStore::add);
        vegaApiClient.getPositions(partyId).forEach(positionStore::add);
    }
}