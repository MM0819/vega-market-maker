package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
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
    private final VegaApiClient vegaApiClient;
    private final String marketId;

    public UpdateLiquidityProvisionTask(@Value("${vega.market.id}") String marketId,
                                        MarketService marketService,
                                        AccountService accountService,
                                        PositionService positionService,
                                        AppConfigStore appConfigStore,
                                        VegaApiClient vegaApiClient,
                                        ReferencePriceStore referencePriceStore) {
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.appConfigStore = appConfigStore;
        this.referencePriceStore = referencePriceStore;
        this.vegaApiClient = vegaApiClient;
        this.marketId = marketId;
    }

    @Override
    public String getCronExpression() {
        return "0 * * * * *";
    }

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
        BigDecimal askPoolSize = bidPoolSize.divide(referencePrice, 4, RoundingMode.HALF_DOWN);
        BigDecimal openVolumeRatio = exposure.divide(askPoolSize, 4, RoundingMode.HALF_DOWN);
        double offerScalingFactor = PricingUtils.getAskScalingFactor(
                exposure.longValue(), openVolumeRatio.doubleValue());
        double bidScalingFactor = PricingUtils.getBidScalingFactor(
                exposure.longValue(), openVolumeRatio.doubleValue());
        List<DistributionStep> askDistribution = PricingUtils.getAskDistribution(
                offerScalingFactor, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getAskQuoteRange() * 2, 1);
        List<DistributionStep> bidDistribution = PricingUtils.getBidDistribution(
                bidScalingFactor, bidPoolSize.doubleValue(), askPoolSize.doubleValue(),
                config.getBidQuoteRange() * 2, 1);
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
                .setFee(BigDecimal.valueOf(0.001)) // TODO - where should this come from?
                .setBids(liquidityProvisionBids)
                .setAsks(liquidityProvisionAsks);
        vegaApiClient.submitLiquidityProvision(liquidityProvision); // TODO - amend if we already have LP order
    }
}