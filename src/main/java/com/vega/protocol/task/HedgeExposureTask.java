package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.ExchangeApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.exchange.Position;
import com.vega.protocol.model.trading.ReferencePrice;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    private final PositionService positionService;
    private final IGApiClient igApiClient;
    private final BinanceApiClient binanceApiClient;
    private final SleepUtils sleepUtils;

    public HedgeExposureTask(DataInitializer dataInitializer,
                             WebSocketInitializer webSocketInitializer,
                             PositionService positionService,
                             IGApiClient igApiClient,
                             BinanceApiClient binanceApiClient,
                             ReferencePriceStore referencePriceStore,
                             SleepUtils sleepUtils) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.positionService = positionService;
        this.igApiClient = igApiClient;
        this.binanceApiClient = binanceApiClient;
        this.sleepUtils = sleepUtils;
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
        double exposure = positionService.getExposure(marketConfig.getMarketId(), marketConfig.getPartyId());
        if(exposure != 0) {
            log.info("Hedging exposure...");
            ExchangeApiClient exchangeApiClient = marketConfig.getReferencePriceSource()
                    .equals(ReferencePriceSource.BINANCE) ? binanceApiClient : igApiClient;
            double hedgeExposure = exchangeApiClient.getPosition(marketConfig.getHedgeSymbol())
                    .orElse(new Position().setSize(0)).getSize();
            double diff = Math.abs(hedgeExposure - exposure);
            if(Math.abs(hedgeExposure) < Math.abs(exposure)) {
                MarketSide side = exposure < 0 ? MarketSide.BUY : MarketSide.SELL;
                executeTwap(side, marketConfig.getHedgeSymbol(), diff, exchangeApiClient);
            } else if(Math.abs(hedgeExposure) > Math.abs(exposure)) {
                MarketSide side = exposure < 0 ? MarketSide.SELL : MarketSide.BUY;
                executeTwap(side, marketConfig.getHedgeSymbol(), diff, exchangeApiClient);
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
        final double totalSize,
        final ExchangeApiClient exchangeApiClient
    ) {
        log.info("TWAP >> {} {} {}", side, totalSize, symbol);
        double remainingSize = totalSize;
        while(remainingSize > 0) {
            ReferencePrice referencePrice = referencePriceStore.get()
                    .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
            double size = side.equals(MarketSide.BUY) ? referencePrice.getAskSize() : referencePrice.getBidSize();
            if(size > remainingSize) {
                size = remainingSize;
                remainingSize = 0.0;
            } else {
                remainingSize = remainingSize - size;
            }
            exchangeApiClient.submitMarketOrder(symbol, size, side);
            log.info("TWAP >> Remaining size = {}", remainingSize);
            sleepUtils.sleep(500L);
        }
    }
}