package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.Market;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.store.MarketStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MarketService {

    private final MarketStore marketStore;
    private final MarketConfigRepository marketConfigRepository;
    private final TradingConfigRepository tradingConfigRepository;
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

    public MarketService(MarketStore marketStore,
                         MarketConfigRepository marketConfigRepository,
                         TradingConfigRepository tradingConfigRepository,
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
        this.marketStore = marketStore;
        this.marketConfigRepository = marketConfigRepository;
        this.tradingConfigRepository = tradingConfigRepository;
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
     * Get market by ID
     *
     * @return {@link Market}
     */
    public Market getById(
            final String marketId
    ) {
        return marketStore.getItems().stream().filter(m -> m.getId().equals(marketId)).findFirst()
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
    }

    /**
     * Create new market config
     *
     * @param marketConfig {@link MarketConfig}
     *
     * @return {@link MarketConfig}
     */
    public MarketConfig createConfig(
            final MarketConfig marketConfig
    ) {
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
        return tradingConfigRepository.save(tradingConfig).getMarketConfig();
    }
}