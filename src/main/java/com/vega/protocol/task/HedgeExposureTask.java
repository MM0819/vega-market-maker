package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.ExchangeApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.Position;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    private final PositionService positionService;
    private final IGApiClient igApiClient;
    private final BinanceApiClient binanceApiClient;
    private final String marketId;
    private final ReferencePriceSource referencePriceSource;
    private final String referencePriceMarket;
    private final String igMarketEpic;
    private final SleepUtils sleepUtils;

    public HedgeExposureTask(DataInitializer dataInitializer,
                             WebSocketInitializer webSocketInitializer,
                             @Value("${vega.market.id}") String marketId,
                             @Value("${hedge.exposure.enabled}") Boolean taskEnabled,
                             @Value("${reference.price.source}") ReferencePriceSource referencePriceSource,
                             @Value("${ig.market.epic}") String igMarketEpic,
                             @Value("${reference.price.market}") String referencePriceMarket,
                             PositionService positionService,
                             IGApiClient igApiClient,
                             BinanceApiClient binanceApiClient,
                             ReferencePriceStore referencePriceStore,
                             SleepUtils sleepUtils) {
        super(dataInitializer, webSocketInitializer, referencePriceStore, taskEnabled);
        this.positionService = positionService;
        this.marketId = marketId;
        this.referencePriceSource = referencePriceSource;
        this.igApiClient = igApiClient;
        this.binanceApiClient = binanceApiClient;
        this.referencePriceMarket = referencePriceMarket;
        this.igMarketEpic = igMarketEpic;
        this.sleepUtils = sleepUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCronExpression() {
        return "0 * * * * *";
    }

    /**
     * {@inheritDoc}
     */
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
        BigDecimal exposure = positionService.getExposure(marketId);
        if(exposure.doubleValue() != 0) {
            log.info("Hedging exposure...");
            ExchangeApiClient exchangeApiClient = referencePriceSource.equals(ReferencePriceSource.BINANCE) ?
                    binanceApiClient : igApiClient;
            String marketSymbol = referencePriceSource.equals(ReferencePriceSource.BINANCE) ?
                    referencePriceMarket : igMarketEpic;
            BigDecimal hedgeExposure = exchangeApiClient.getPosition(marketSymbol)
                    .orElse(new Position().setSize(BigDecimal.ZERO)).getSize();
            BigDecimal diff = hedgeExposure.subtract(exposure).abs();
            if(hedgeExposure.abs().doubleValue() < exposure.abs().doubleValue()) {
                MarketSide side = exposure.doubleValue() < 0 ? MarketSide.BUY : MarketSide.SELL;
                executeTwap(side, marketSymbol, diff, exchangeApiClient);
            } else if(hedgeExposure.abs().doubleValue() > exposure.abs().doubleValue()) {
                MarketSide side = exposure.doubleValue() < 0 ? MarketSide.SELL : MarketSide.BUY;
                executeTwap(side, marketSymbol, diff, exchangeApiClient);
            }
        }
    }

    /**
     * Execute a TWAP trade
     *
     * @param side {@link MarketSide}
     * @param symbol the market symbol
     * @param totalSize the total trade size
     * @param exchangeApiClient {@link ExchangeApiClient}
     */
    private void executeTwap(
        final MarketSide side,
        final String symbol,
        final BigDecimal totalSize,
        final ExchangeApiClient exchangeApiClient
    ) {
        log.info("TWAP >> {} {} {}", side, totalSize, symbol);
        BigDecimal remainingSize = totalSize;
        while(remainingSize.doubleValue() > 0) {
            ReferencePrice referencePrice = referencePriceStore.get()
                    .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
            BigDecimal size = side.equals(MarketSide.BUY) ? referencePrice.getAskSize() : referencePrice.getBidSize();
            if(size.doubleValue() > remainingSize.doubleValue()) {
                size = remainingSize;
                remainingSize = BigDecimal.ZERO;
            } else {
                remainingSize = remainingSize.subtract(size);
            }
            exchangeApiClient.submitMarketOrder(symbol, size, side);
            log.info("TWAP >> Remaining size = {}", remainingSize);
            sleepUtils.sleep(500L);
        }
    }
}