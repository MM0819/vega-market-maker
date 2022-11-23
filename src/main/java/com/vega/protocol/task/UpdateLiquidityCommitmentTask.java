package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketState;
import com.vega.protocol.constant.PeggedReference;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.LiquidityCommitment;
import com.vega.protocol.model.LiquidityCommitmentOffset;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class UpdateLiquidityCommitmentTask extends TradingTask {

    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final VegaApiClient vegaApiClient;
    private final TradingConfigRepository tradingConfigRepository;

    public UpdateLiquidityCommitmentTask(MarketService marketService,
                                         AccountService accountService,
                                         PositionService positionService,
                                         VegaApiClient vegaApiClient,
                                         ReferencePriceStore referencePriceStore,
                                         LiquidityCommitmentStore liquidityCommitmentStore,
                                         DataInitializer dataInitializer,
                                         WebSocketInitializer webSocketInitializer,
                                         TradingConfigRepository tradingConfigRepository) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.vegaApiClient = vegaApiClient;
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
        Market market = marketService.getById(marketId);
        if(!market.getState().equals(MarketState.ACTIVE)) {
            log.warn("Cannot trade; market state = {}", market.getState());
            return;
        }
        double balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance == 0) {
            log.info("Cannot update liquidity commitment because balance = {}", balance);
            return;
        }
        double exposure = positionService.getExposure(marketId);
        TradingConfig tradingConfig = tradingConfigRepository.findByMarketConfig(marketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        double midPrice = referencePrice.getMidPrice();
        double commitmentAmount = balance * 0.5 * tradingConfig.getCommitmentBalanceRatio();
        double requiredStake = market.getTargetStake() * (1 + tradingConfig.getStakeBuffer());
        log.info("Exposure = {}\nRequired stake = {}", exposure, requiredStake);
        if(requiredStake > commitmentAmount && requiredStake < balance) {
            commitmentAmount = requiredStake;
        }
        List<LiquidityCommitmentOffset> bids = new ArrayList<>();
        List<LiquidityCommitmentOffset> asks = new ArrayList<>();
        for(int i=0; i<tradingConfig.getCommitmentOrderCount(); i++) {
            LiquidityCommitmentOffset bidOffset = new LiquidityCommitmentOffset()
                    .setOffset(midPrice * tradingConfig.getCommitmentSpread() * (i+1))
                    .setProportion(1)
                    .setReference(PeggedReference.MID);
            LiquidityCommitmentOffset askOffset = new LiquidityCommitmentOffset()
                    .setOffset(midPrice * tradingConfig.getCommitmentSpread() * (i+1))
                    .setProportion(1)
                    .setReference(PeggedReference.MID);
            bids.add(bidOffset);
            asks.add(askOffset);
        }
        LiquidityCommitment liquidityCommitment = new LiquidityCommitment()
                .setMarket(market)
                .setCommitmentAmount(commitmentAmount)
                .setFee(tradingConfig.getFee())
                .setBids(bids)
                .setAsks(asks);
        Optional<LiquidityCommitment> currentCommitment = liquidityCommitmentStore.getItems().stream()
                .filter(c -> c.getMarket().getId().equals(marketId)).findFirst();
        boolean hasCommitment = currentCommitment.isPresent();
        if(hasCommitment) {
            double stakeFromOthers = market.getSuppliedStake() - currentCommitment.get().getCommitmentAmount();
            commitmentAmount = commitmentAmount - stakeFromOthers;
        }
        vegaApiClient.submitLiquidityCommitment(liquidityCommitment, partyId, hasCommitment);
        log.info("Liquidity commitment updated -> {}", commitmentAmount);
    }
}
