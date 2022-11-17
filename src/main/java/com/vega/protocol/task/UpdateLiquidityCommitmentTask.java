package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.PeggedReference;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.utils.PricingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UpdateLiquidityCommitmentTask extends TradingTask {

    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final AppConfigStore appConfigStore;
    private final ReferencePriceStore referencePriceStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final VegaApiClient vegaApiClient;
    private final String marketId;
    private final String partyId;
    private final String updateLiquidityCommitmentCronExpression;
    private final PricingUtils pricingUtils;

    public UpdateLiquidityCommitmentTask(@Value("${vega.market.id}") String marketId,
                                         @Value("${update.liquidity.commitment.enabled}") Boolean taskEnabled,
                                         @Value("${vega.party.id}") String partyId,
                                         MarketService marketService,
                                         AccountService accountService,
                                         PositionService positionService,
                                         AppConfigStore appConfigStore,
                                         VegaApiClient vegaApiClient,
                                         ReferencePriceStore referencePriceStore,
                                         LiquidityCommitmentStore liquidityCommitmentStore,
                                         PricingUtils pricingUtils,
                                         DataInitializer dataInitializer,
                                         WebSocketInitializer webSocketInitializer,
                                         @Value("${update.liquidity.commitment.cron.expression}") String updateLiquidityCommitmentCronExpression) {
        super(dataInitializer, webSocketInitializer, taskEnabled);
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.appConfigStore = appConfigStore;
        this.referencePriceStore = referencePriceStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.vegaApiClient = vegaApiClient;
        this.marketId = marketId;
        this.updateLiquidityCommitmentCronExpression = updateLiquidityCommitmentCronExpression;
        this.partyId = partyId;
        this.pricingUtils = pricingUtils;
    }

    @Override
    public String getCronExpression() {
        return updateLiquidityCommitmentCronExpression;
    }

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
        log.info("Updating liquidity commitment...");
        Market market = marketService.getById(marketId);
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance.doubleValue() == 0) {
            log.info("Cannot update liquidity commitment because balance = {}", balance);
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
        BigDecimal commitmentAmount = bidPoolSize.multiply(BigDecimal.valueOf(config.getCommitmentBalanceRatio()));
        BigDecimal requiredStake = (market.getTargetStake().multiply(BigDecimal.valueOf(1 + config.getStakeBuffer())));
        log.info("Exposure = {}\nBid pool size = {}\nAsk pool size = {}; Required stake = {}",
                exposure, bidPoolSize, askPoolSize, requiredStake);
        if(requiredStake.doubleValue() > commitmentAmount.doubleValue() &&
                requiredStake.doubleValue() < bidPoolSize.doubleValue()) {
            commitmentAmount = requiredStake;
        }
        List<LiquidityCommitmentOffset> bids = new ArrayList<>();
        List<LiquidityCommitmentOffset> asks = new ArrayList<>();
        double scalingFactor = exposure.abs().divide(askPoolSize, 8, RoundingMode.HALF_DOWN).doubleValue();
        int baseProportion = 100000000;
        for(int i=0; i<config.getCommitmentOrderCount(); i++) {
            int bidProportion = (int) (exposure.doubleValue() > 0 ?
                    Math.max(1, baseProportion * (1 - scalingFactor)) : baseProportion);
            int askProportion = (int) (exposure.doubleValue() < 0 ?
                    Math.max(1, baseProportion * (1 - scalingFactor)) : baseProportion);
            LiquidityCommitmentOffset bidOffset = new LiquidityCommitmentOffset()
                    .setOffset(midPrice.multiply(BigDecimal.valueOf(config.getCommitmentSpread() * (i+1))))
                    .setProportion(bidProportion)
                    .setReference(PeggedReference.MID);
            LiquidityCommitmentOffset askOffset = new LiquidityCommitmentOffset()
                    .setOffset(midPrice.multiply(BigDecimal.valueOf(config.getCommitmentSpread() * (i+1))))
                    .setProportion(askProportion)
                    .setReference(PeggedReference.MID);
            bids.add(bidOffset);
            asks.add(askOffset);
        }
        LiquidityCommitment liquidityCommitment = new LiquidityCommitment()
                .setMarket(market)
                .setCommitmentAmount(commitmentAmount)
                .setFee(BigDecimal.valueOf(config.getFee()))
                .setBids(bids)
                .setAsks(asks);
        boolean hasCommitment = liquidityCommitmentStore.getItems().stream()
                .anyMatch(c -> c.getMarket().getId().equals(marketId));
        vegaApiClient.submitLiquidityCommitment(liquidityCommitment, partyId, hasCommitment);
        log.info("Liquidity commitment updated -> {}", commitmentAmount);
    }
}
