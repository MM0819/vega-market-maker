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
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PricingUtils pricingUtils;
    private final String partyId;

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
                                         WebSocketInitializer webSocketInitializer) {
        super(dataInitializer, webSocketInitializer, taskEnabled);
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.appConfigStore = appConfigStore;
        this.referencePriceStore = referencePriceStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.vegaApiClient = vegaApiClient;
        this.marketId = marketId;
        this.pricingUtils = pricingUtils;
        this.partyId = partyId;
    }

    @Override
    public String getCronExpression() {
        return "0 * * * * *";
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
        BigDecimal referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND)).getMidPrice();
        BigDecimal bidPoolSize = balance.multiply(BigDecimal.valueOf(0.5));
        BigDecimal askPoolSize = bidPoolSize.divide(referencePrice, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        BigDecimal openVolumeRatio = exposure.abs().divide(askPoolSize,
                market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        double scalingFactor = pricingUtils.getScalingFactor(openVolumeRatio.doubleValue());
        log.info("Exposure = {}\nBid pool size = {}\nAsk pool size = {}", exposure, bidPoolSize, askPoolSize);
        List<DistributionStep> askDistribution = pricingUtils.getAskDistribution(
                exposure.doubleValue() < 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getAskQuoteRange(), config.getOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getBidDistribution(
                exposure.doubleValue() > 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getBidQuoteRange(), config.getOrderCount());
        BigDecimal commitmentAmount = BigDecimal.valueOf((config.getAskQuoteRange() + config.getBidQuoteRange()) / 4)
                .multiply(balance);
        List<LiquidityCommitmentOffset> liquidityCommitmentBids = bidDistribution.stream()
                .map(d -> new LiquidityCommitmentOffset()
                        .setReference(PeggedReference.MID)
                        .setOffset(referencePrice.subtract(BigDecimal.valueOf(d.getPrice())))
                        .setProportion(d.getSize().intValue()))
                .collect(Collectors.toList());
        List<LiquidityCommitmentOffset> liquidityCommitmentAsks = askDistribution.stream()
                .map(d -> new LiquidityCommitmentOffset()
                        .setReference(PeggedReference.MID)
                        .setOffset(referencePrice.subtract(BigDecimal.valueOf(d.getPrice())))
                        .setProportion(d.getSize().intValue()))
                .collect(Collectors.toList());
        LiquidityCommitment liquidityCommitment = new LiquidityCommitment()
                .setCommitmentAmount(commitmentAmount)
                .setFee(BigDecimal.valueOf(config.getFee()))
                .setBids(liquidityCommitmentBids)
                .setAsks(liquidityCommitmentAsks);
        boolean hasCommitment = liquidityCommitmentStore.getItems().stream()
                .anyMatch(c -> c.getMarket().getId().equals(marketId));
        vegaApiClient.submitLiquidityCommitment(liquidityCommitment, partyId, hasCommitment);
        log.info("Liquidity commitment successfully updated!");
    }
}