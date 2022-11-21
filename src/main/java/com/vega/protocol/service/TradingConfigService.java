package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.TaskInitializer;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.request.CreateTradingConfigRequest;
import com.vega.protocol.request.UpdateTradingConfigRequest;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.task.HedgeExposureTask;
import com.vega.protocol.task.NaiveFlowTask;
import com.vega.protocol.task.UpdateLiquidityCommitmentTask;
import com.vega.protocol.task.UpdateQuotesTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TradingConfigService {
    private final MarketConfigRepository marketConfigRepository;
    private final TradingConfigRepository tradingConfigRepository;
    private final TaskInitializer taskInitializer;
    private final MarketStore marketStore;
    private final Double fee;
    private final Double askQuoteRange;
    private final Double bidQuoteRange;
    private final Double askSizeFactor;
    private final Double bidSizeFactor;
    private final Double bboOffset;
    private final Double commitmentBalanceRatio;
    private final Double commitmentSpread;
    private final Integer commitmentOrderCount;
    private final Integer quoteOrderCount;
    private final Double stakeBuffer;
    private final Double minSpread;
    private final Double maxSpread;

    public TradingConfigService(MarketConfigRepository marketConfigRepository,
                                TradingConfigRepository tradingConfigRepository,
                                TaskInitializer taskInitializer,
                                MarketStore marketStore,
                                @Value("${fee}") Double fee,
                                @Value("${ask.quote.range}") Double askQuoteRange,
                                @Value("${bid.quote.range}") Double bidQuoteRange,
                                @Value("${ask.size.factor}") Double askSizeFactor,
                                @Value("${bid.size.factor}") Double bidSizeFactor,
                                @Value("${bbo.offset}") Double bboOffset,
                                @Value("${commitment.balance.ratio}") Double commitmentBalanceRatio,
                                @Value("${commitment.spread}") Double commitmentSpread,
                                @Value("${commitment.order.count}") Integer commitmentOrderCount,
                                @Value("${quote.order.count}") Integer quoteOrderCount,
                                @Value("${stake.buffer}") Double stakeBuffer,
                                @Value("${min.spread}") Double minSpread,
                                @Value("${max.spread}") Double maxSpread) {
        this.marketConfigRepository = marketConfigRepository;
        this.tradingConfigRepository = tradingConfigRepository;
        this.taskInitializer = taskInitializer;
        this.marketStore = marketStore;
        this.fee = fee;
        this.askQuoteRange = askQuoteRange;
        this.bidQuoteRange = bidQuoteRange;
        this.askSizeFactor = askSizeFactor;
        this.bidSizeFactor = bidSizeFactor;
        this.bboOffset = bboOffset;
        this.commitmentBalanceRatio = commitmentBalanceRatio;
        this.commitmentSpread = commitmentSpread;
        this.commitmentOrderCount = commitmentOrderCount;
        this.quoteOrderCount = quoteOrderCount;
        this.stakeBuffer = stakeBuffer;
        this.minSpread = minSpread;
        this.maxSpread = maxSpread;
    }


    /**
     * Get all trading config items
     *
     * @return {@link List < TradingConfig >}
     */
    public List<TradingConfig> get() {
        return tradingConfigRepository.findAll();
    }

    /**
     * Create new trading config
     *
     * @param request {@link CreateTradingConfigRequest}
     *
     * @return {@link TradingConfig}
     */
    public TradingConfig create(
            final CreateTradingConfigRequest request
    ) {
        validate(request);
        MarketConfig marketConfig = new MarketConfig()
                .setMarketId(request.getMarketId())
                .setPartyId(request.getPartyId())
                .setAllocatedMargin(request.getAllocatedMargin())
                .setHedgeSymbol(request.getHedgeSymbol())
                .setReferencePriceSymbol(request.getReferencePriceSymbol())
                .setReferencePriceSource(request.getReferencePriceSource())
                .setUpdateHedgeEnabled(false)
                .setHedgeFee(request.getHedgeFee())
                .setTargetEdge(request.getTargetEdge())
                .setUpdateLiquidityCommitmentEnabled(false)
                .setUpdateNaiveFlowEnabled(false)
                .setUpdateQuotesEnabled(false)
                .setUpdateHedgeFrequency(60)
                .setUpdateLiquidityCommitmentFrequency(60)
                .setUpdateNaiveFlowFrequency(60)
                .setUpdateQuotesFrequency(60);
        TradingConfig tradingConfig = new TradingConfig()
                .setId(UUID.randomUUID())
                .setMarketConfig(marketConfigRepository.save(marketConfig.setId(UUID.randomUUID())))
                .setFee(fee)
                .setAskQuoteRange(askQuoteRange)
                .setBidQuoteRange(bidQuoteRange)
                .setAskSizeFactor(askSizeFactor)
                .setBidSizeFactor(bidSizeFactor)
                .setBboOffset(bboOffset)
                .setCommitmentBalanceRatio(commitmentBalanceRatio)
                .setCommitmentSpread(commitmentSpread)
                .setCommitmentOrderCount(commitmentOrderCount)
                .setQuoteOrderCount(quoteOrderCount)
                .setStakeBuffer(stakeBuffer)
                .setMinSpread(minSpread)
                .setMaxSpread(maxSpread);
        return tradingConfigRepository.save(tradingConfig);
    }

    /**
     * Update trading config for given market
     *
     * @param request {@link UpdateTradingConfigRequest}
     *
     * @return {@link TradingConfig}
     */
    public TradingConfig update(
            final UpdateTradingConfigRequest request
    ) {
        MarketConfig currentMarketConfig = marketConfigRepository.findByMarketId(request.getMarketId())
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_CONFIG_NOT_FOUND));
        TradingConfig currentTradingConfig = tradingConfigRepository.findByMarketConfig(currentMarketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        validate(request, currentTradingConfig, currentMarketConfig);
        UUID id = currentMarketConfig.getId();
        if(!currentMarketConfig.getUpdateNaiveFlowEnabled()) {
            taskInitializer.cancelTask(id, NaiveFlowTask.class);
        } else {
            taskInitializer.scheduleTask(true, currentMarketConfig.getUpdateNaiveFlowFrequency(),
                    currentMarketConfig, NaiveFlowTask.class);
        }
        if(!currentMarketConfig.getUpdateQuotesEnabled()) {
            taskInitializer.cancelTask(id, UpdateQuotesTask.class);
        } else {
            taskInitializer.scheduleTask(true, currentMarketConfig.getUpdateQuotesFrequency(),
                    currentMarketConfig, UpdateQuotesTask.class);
        }
        if(!currentMarketConfig.getUpdateLiquidityCommitmentEnabled()) {
            taskInitializer.cancelTask(id, UpdateLiquidityCommitmentTask.class);
        } else {
            taskInitializer.scheduleTask(true, currentMarketConfig.getUpdateLiquidityCommitmentFrequency(),
                    currentMarketConfig, UpdateLiquidityCommitmentTask.class);
        }
        if(!currentMarketConfig.getUpdateHedgeEnabled()) {
            taskInitializer.cancelTask(id, HedgeExposureTask.class);
        } else {
            taskInitializer.scheduleTask(true, currentMarketConfig.getUpdateHedgeFrequency(),
                    currentMarketConfig, HedgeExposureTask.class);
        }
        return tradingConfigRepository.save(currentTradingConfig);
    }

    /**
     * Delete trading config for given market
     *
     * @param marketId the market ID
     *
     * @return true if deleted
     */
    public boolean delete(
            final String marketId
    ) {
        MarketConfig currentMarketConfig = marketConfigRepository.findByMarketId(marketId)
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_CONFIG_NOT_FOUND));
        TradingConfig currentTradingConfig = tradingConfigRepository.findByMarketConfig(currentMarketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        try {
            tradingConfigRepository.delete(currentTradingConfig);
            marketConfigRepository.delete(currentMarketConfig);
            UUID id = currentMarketConfig.getId();
            taskInitializer.cancelTask(id, HedgeExposureTask.class);
            taskInitializer.cancelTask(id, UpdateQuotesTask.class);
            taskInitializer.cancelTask(id, UpdateLiquidityCommitmentTask.class);
            taskInitializer.cancelTask(id, NaiveFlowTask.class);
            return true;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Validate {@link CreateTradingConfigRequest}
     *
     * @param request {@link CreateTradingConfigRequest}
     */
    public void validate(
            final CreateTradingConfigRequest request
    ) {
        if(ObjectUtils.isEmpty(request.getAllocatedMargin())) {
            throw new TradingException(ErrorCode.ALLOCATED_MARGIN_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getPartyId())) {
            throw new TradingException(ErrorCode.PARTY_ID_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getMarketId())) {
            throw new TradingException(ErrorCode.MARKET_ID_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getReferencePriceSymbol())) {
            throw new TradingException(ErrorCode.REFERENCE_PRICE_SYMBOL_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getReferencePriceSource())) {
            throw new TradingException(ErrorCode.REFERENCE_PRICE_SOURCE_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getHedgeSymbol())) {
            throw new TradingException(ErrorCode.HEDGE_SYMBOL_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getHedgeFee())) {
            throw new TradingException(ErrorCode.HEDGE_FEE_MANDATORY);
        }
        if(ObjectUtils.isEmpty(request.getTargetEdge())) {
            throw new TradingException(ErrorCode.TARGET_EDGE_MANDATORY);
        }
        boolean configExists = marketConfigRepository.findByMarketId(request.getMarketId()).isPresent();
        if(configExists) {
            throw new TradingException(ErrorCode.MARKET_CONFIG_ALREADY_EXISTS);
        }
        boolean validMarketId = marketStore.getById(request.getMarketId()).isPresent();
        if(!validMarketId) {
            throw new TradingException(ErrorCode.INVALID_MARKET_ID);
        }
    }

    /**
     * Validate {@link UpdateTradingConfigRequest}
     *
     * @param request {@link UpdateTradingConfigRequest}
     * @param currentTradingConfig {@link TradingConfig}
     * @param currentMarketConfig {@link MarketConfig}
     */
    public void validate(
            final UpdateTradingConfigRequest request,
            final TradingConfig currentTradingConfig,
            final MarketConfig currentMarketConfig
    ) {
        if(!ObjectUtils.isEmpty(request.getFee())) {
            currentTradingConfig.setFee(request.getFee());
        }
        if(!ObjectUtils.isEmpty(request.getUpdateHedgeFrequency())) {
            currentMarketConfig.setUpdateHedgeFrequency(request.getUpdateHedgeFrequency());
        }
        // TODO - handle all fields
    }
}