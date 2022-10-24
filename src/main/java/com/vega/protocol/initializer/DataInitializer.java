package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final OrderStore orderStore;
    private final MarketStore marketStore;
    private final PositionStore positionStore;
    private final AppConfigStore appConfigStore;
    private final AccountStore accountStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final AssetStore assetStore;
    private final VegaApiClient vegaApiClient;
    private final String partyId;
    private final String marketId;
    private final Double fee;
    private final Double spread;
    private final Integer orderCount;
    private final Double bidSizeFactor;
    private final Double askSizeFactor;
    private final Double bidQuoteRange;
    private final Double askQuoteRange;
    private final Double pricingStepSize;

    @Getter
    private boolean initialized = false;

    public DataInitializer(OrderStore orderStore,
                           MarketStore marketStore,
                           PositionStore positionStore,
                           AppConfigStore appConfigStore,
                           AccountStore accountStore,
                           LiquidityCommitmentStore liquidityCommitmentStore,
                           AssetStore assetStore,
                           VegaApiClient vegaApiClient,
                           @Value("${vega.party.id}") String partyId,
                           @Value("${vega.market.id}") String marketId,
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
        this.accountStore = accountStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.assetStore = assetStore;
        this.vegaApiClient = vegaApiClient;
        this.partyId = partyId;
        this.marketId = marketId;
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
        vegaApiClient.getAssets().forEach(assetStore::add);
        vegaApiClient.getMarkets().forEach(marketStore::add);
        vegaApiClient.getAccounts(partyId).forEach(accountStore::add);
        vegaApiClient.getPositions(partyId).forEach(positionStore::add);
        vegaApiClient.getOpenOrders(partyId).forEach(orderStore::add);
        vegaApiClient.getLiquidityCommitments(partyId).forEach(liquidityCommitmentStore::add);
        initialized = true;
    }
}