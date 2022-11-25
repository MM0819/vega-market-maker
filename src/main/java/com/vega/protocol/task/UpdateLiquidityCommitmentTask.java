package com.vega.protocol.task;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.trading.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.*;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vega.Assets;
import vega.Markets;
import vega.Vega;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UpdateLiquidityCommitmentTask extends TradingTask {

    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final AssetService assetService;
    private final LiquidityProvisionService liquidityProvisionService;
    private final VegaGrpcClient vegaGrpcClient;
    private final DecimalUtils decimalUtils;
    private final TradingConfigRepository tradingConfigRepository;

    public UpdateLiquidityCommitmentTask(MarketService marketService,
                                         AccountService accountService,
                                         PositionService positionService,
                                         AssetService assetService,
                                         LiquidityProvisionService liquidityProvisionService,
                                         VegaGrpcClient vegaGrpcClient,
                                         ReferencePriceStore referencePriceStore,
                                         DataInitializer dataInitializer,
                                         WebSocketInitializer webSocketInitializer,
                                         DecimalUtils decimalUtils,
                                         TradingConfigRepository tradingConfigRepository) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.assetService = assetService;
        this.liquidityProvisionService = liquidityProvisionService;
        this.vegaGrpcClient = vegaGrpcClient;
        this.decimalUtils = decimalUtils;
        this.tradingConfigRepository = tradingConfigRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(
            final MarketConfig marketConfig
    ) {
        if(!isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        String marketId = marketConfig.getMarketId();
        String partyId = marketConfig.getPartyId();
        log.info("Updating liquidity commitment...");
        Markets.Market market = marketService.getById(marketId);
        if(!market.getState().equals(Markets.Market.State.STATE_ACTIVE)) {
            log.warn("Cannot trade; market state = {}", market.getState());
            return;
        }
        Assets.Asset asset = assetService.getById(market.getTradableInstrument()
                .getInstrument().getFuture().getSettlementAsset());
        Vega.MarketData marketData = marketService.getDataById(marketId);
        double balance = accountService.getTotalBalance(market.getTradableInstrument()
                .getInstrument().getFuture().getSettlementAsset());
        if(balance == 0) {
            log.info("Cannot update liquidity commitment because balance = {}", balance);
            return;
        }
        double exposure = positionService.getExposure(marketId, partyId);
        TradingConfig tradingConfig = tradingConfigRepository.findByMarketConfig(marketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        double midPrice = referencePrice.getMidPrice();
        double commitmentAmount = balance * 0.5 * tradingConfig.getCommitmentBalanceRatio();
        double targetStake = decimalUtils.convertToDecimals(asset.getDetails().getDecimals(),
                new BigDecimal(marketData.getTargetStake()));
        double suppliedStake = decimalUtils.convertToDecimals(asset.getDetails().getDecimals(),
                new BigDecimal(marketData.getSuppliedStake()));
        double requiredStake = targetStake * (1 + tradingConfig.getStakeBuffer());
        log.info("Exposure = {}\nRequired stake = {}", exposure, requiredStake);
        if(requiredStake > commitmentAmount && requiredStake < balance) {
            commitmentAmount = requiredStake;
        }
        List<Vega.LiquidityOrder> buys = new ArrayList<>();
        List<Vega.LiquidityOrder> sells = new ArrayList<>();
        for(int i=0; i<tradingConfig.getCommitmentOrderCount(); i++) {
            double offset = midPrice * tradingConfig.getCommitmentSpread() * (i+1);
            String vegaOffset = decimalUtils.convertFromDecimals(
                    market.getDecimalPlaces(), offset).toBigInteger().toString();
            var buyOrder = Vega.LiquidityOrder.newBuilder()
                    .setOffset(vegaOffset)
                    .setProportion(1)
                    .setReference(Vega.PeggedReference.PEGGED_REFERENCE_BEST_BID)
                    .build();
            var sellOrder = Vega.LiquidityOrder.newBuilder()
                    .setOffset(vegaOffset)
                    .setProportion(1)
                    .setReference(Vega.PeggedReference.PEGGED_REFERENCE_BEST_ASK)
                    .build();
            buys.add(buyOrder);
            sells.add(sellOrder);
        }
        var lp = liquidityProvisionService.getByMarketIdAndPartyId(marketId, partyId);
        boolean hasCommitment = lp.isPresent();
        if(hasCommitment) {
            double currentCommitment = decimalUtils.convertToDecimals(asset.getDetails().getDecimals(),
                    new BigDecimal(lp.get().getCommitmentAmount()));
            double stakeFromOthers = suppliedStake - currentCommitment;
            commitmentAmount = commitmentAmount - stakeFromOthers;
        }
        String vegaCommitmentAmount = decimalUtils.convertFromDecimals(
                asset.getDetails().getDecimals(), commitmentAmount).toBigInteger().toString();
        String vegaFee = String.valueOf(tradingConfig.getFee());
        vegaGrpcClient.submitLiquidityProvision(buys, sells, vegaCommitmentAmount, vegaFee, marketId, partyId);
        log.info("Liquidity commitment updated -> {}", commitmentAmount);
    }
}
