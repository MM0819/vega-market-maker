package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.LiquidityProvisionStore;
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
public class UpdateLiquidityProvisionTask extends TradingTask {

    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final AppConfigStore appConfigStore;
    private final ReferencePriceStore referencePriceStore;
    private final LiquidityProvisionStore liquidityProvisionStore;
    private final VegaApiClient vegaApiClient;
    private final String marketId;
    private final PricingUtils pricingUtils;
    private final String partyId;

    public UpdateLiquidityProvisionTask(@Value("${vega.market.id}") String marketId,
                                        @Value("${vega.party.id}") String partyId,
                                        MarketService marketService,
                                        AccountService accountService,
                                        PositionService positionService,
                                        AppConfigStore appConfigStore,
                                        VegaApiClient vegaApiClient,
                                        ReferencePriceStore referencePriceStore,
                                        LiquidityProvisionStore liquidityProvisionStore,
                                        PricingUtils pricingUtils) {
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.appConfigStore = appConfigStore;
        this.referencePriceStore = referencePriceStore;
        this.liquidityProvisionStore = liquidityProvisionStore;
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
        Market market = marketService.getById(marketId);
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance.doubleValue() == 0) {
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
        List<DistributionStep> askDistribution = pricingUtils.getAskDistribution(
                exposure.doubleValue() < 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getAskQuoteRange(), config.getOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getBidDistribution(
                exposure.doubleValue() > 0 ? scalingFactor : 1.0, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getBidQuoteRange(), config.getOrderCount());
        BigDecimal commitment = BigDecimal.valueOf((config.getAskQuoteRange() + config.getBidQuoteRange()) / 4)
                .multiply(balance);
        List<LiquidityProvisionOffset> liquidityProvisionBids = bidDistribution.stream()
                .map(d -> new LiquidityProvisionOffset()
                        .setOffset(referencePrice.subtract(BigDecimal.valueOf(d.getPrice())).longValue())
                        .setPortion(d.getSize().longValue()))
                .collect(Collectors.toList());
        List<LiquidityProvisionOffset> liquidityProvisionAsks = askDistribution.stream()
                .map(d -> new LiquidityProvisionOffset()
                        .setOffset(referencePrice.subtract(BigDecimal.valueOf(d.getPrice())).longValue())
                        .setPortion(d.getSize().longValue()))
                .collect(Collectors.toList());
        LiquidityProvision liquidityProvision = new LiquidityProvision()
                .setCommitment(commitment)
                .setFee(BigDecimal.valueOf(config.getFee()))
                .setBids(liquidityProvisionBids)
                .setAsks(liquidityProvisionAsks);
        if(liquidityProvisionStore.get().isPresent()) {
            vegaApiClient.amendLiquidityProvision(liquidityProvision, partyId);
        } else {
            vegaApiClient.submitLiquidityProvision(liquidityProvision, partyId);
        }
    }
}