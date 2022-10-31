package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer {

    private final OrderStore orderStore;
    private final MarketStore marketStore;
    private final PositionStore positionStore;
    private final AppConfigStore appConfigStore;
    private final AccountStore accountStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final AssetStore assetStore;
    private final NetworkParameterStore networkParameterStore;
    private final VegaApiClient vegaApiClient;
    private final String partyId;
    private final Double fee;
    private final Double minSpread;
    private final Double maxSpread;
    private final Integer orderCount;
    private final Double bidSizeFactor;
    private final Double askSizeFactor;
    private final Double commitmentBalanceRatio;
    private final Double bidQuoteRange;
    private final Double askQuoteRange;
    private final Double pricingStepSize;
    private final Double commitmentSpread;
    private final Integer commitmentOrderCount;
    private final Double stakeBuffer;

    @Getter
    private boolean initialized = false;

    public DataInitializer(OrderStore orderStore,
                           MarketStore marketStore,
                           PositionStore positionStore,
                           AppConfigStore appConfigStore,
                           AccountStore accountStore,
                           LiquidityCommitmentStore liquidityCommitmentStore,
                           AssetStore assetStore,
                           NetworkParameterStore networkParameterStore,
                           VegaApiClient vegaApiClient,
                           @Value("${vega.party.id}") String partyId,
                           @Value("${fee}") Double fee,
                           @Value("${min.spread}") Double minSpread,
                           @Value("${max.spread}") Double maxSpread,
                           @Value("${commitment.spread}") Double commitmentSpread,
                           @Value("${order.count}") Integer orderCount,
                           @Value("${bid.size.factor}") Double bidSizeFactor,
                           @Value("${ask.size.factor}") Double askSizeFactor,
                           @Value("${commitment.balance.ratio}") Double commitmentBalanceRatio,
                           @Value("${bid.quote.range}") Double bidQuoteRange,
                           @Value("${ask.quote.range}") Double askQuoteRange,
                           @Value("${pricing.step.size}") Double pricingStepSize,
                           @Value("${commitment.order.count}") Integer commitmentOrderCount,
                           @Value("${stake.buffer}") Double stakeBuffer) {
        this.orderStore = orderStore;
        this.marketStore = marketStore;
        this.positionStore = positionStore;
        this.appConfigStore = appConfigStore;
        this.accountStore = accountStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.assetStore = assetStore;
        this.networkParameterStore = networkParameterStore;
        this.vegaApiClient = vegaApiClient;
        this.partyId = partyId;
        this.fee = fee;
        this.minSpread = minSpread;
        this.maxSpread = maxSpread;
        this.orderCount = orderCount;
        this.commitmentBalanceRatio = commitmentBalanceRatio;
        this.bidSizeFactor = bidSizeFactor;
        this.askSizeFactor = askSizeFactor;
        this.bidQuoteRange = bidQuoteRange;
        this.askQuoteRange = askQuoteRange;
        this.pricingStepSize = pricingStepSize;
        this.commitmentSpread = commitmentSpread;
        this.commitmentOrderCount = commitmentOrderCount;
        this.stakeBuffer = stakeBuffer;
    }

    /**
     * Initialize data
     */
    public void initialize() {
        AppConfig config = new AppConfig()
                .setFee(fee)
                .setMinSpread(minSpread)
                .setMaxSpread(maxSpread)
                .setCommitmentSpread(commitmentSpread)
                .setOrderCount(orderCount)
                .setBidSizeFactor(bidSizeFactor)
                .setAskSizeFactor(askSizeFactor)
                .setCommitmentBalanceRatio(commitmentBalanceRatio)
                .setBidQuoteRange(bidQuoteRange)
                .setAskQuoteRange(askQuoteRange)
                .setPricingStepSize(pricingStepSize)
                .setCommitmentOrderCount(commitmentOrderCount)
                .setStakeBuffer(stakeBuffer);
        appConfigStore.update(config);
        updateState();
        initialized = true;
    }

    private void updateState() {
        vegaApiClient.getNetworkParameters().forEach(networkParameterStore::update);
        vegaApiClient.getAssets().forEach(assetStore::update);
        vegaApiClient.getMarkets().forEach(marketStore::update);
        vegaApiClient.getAccounts(partyId).forEach(accountStore::update);
        vegaApiClient.getPositions(partyId).forEach(positionStore::update);
        vegaApiClient.getOpenOrders(partyId).forEach(orderStore::update);
        vegaApiClient.getLiquidityCommitments(partyId).forEach(liquidityCommitmentStore::update);
    }
}