package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.ExchangeApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.exchange.Position;
import com.vega.protocol.service.OrderService;
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
    private final OrderService orderService;

    public HedgeExposureTask(final DataInitializer dataInitializer,
                             final WebSocketInitializer webSocketInitializer,
                             final PositionService positionService,
                             final IGApiClient igApiClient,
                             final BinanceApiClient binanceApiClient,
                             final ReferencePriceStore referencePriceStore,
                             final OrderService orderService) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.positionService = positionService;
        this.igApiClient = igApiClient;
        this.binanceApiClient = binanceApiClient;
        this.orderService = orderService;
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
                orderService.executeTwap(side, marketConfig.getHedgeSymbol(), diff, exchangeApiClient);
            } else if(Math.abs(hedgeExposure) > Math.abs(exposure)) {
                MarketSide side = exposure < 0 ? MarketSide.SELL : MarketSide.BUY;
                orderService.executeTwap(side, marketConfig.getHedgeSymbol(), diff, exchangeApiClient);
            }
        }
    }
}