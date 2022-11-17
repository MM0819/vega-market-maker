package com.vega.protocol.task;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TradingTask {

    protected final DataInitializer dataInitializer;
    protected final WebSocketInitializer webSocketInitializer;
    protected final ReferencePriceStore referencePriceStore;

    protected TradingTask(DataInitializer dataInitializer,
                          WebSocketInitializer webSocketInitializer,
                          ReferencePriceStore referencePriceStore) {
        this.dataInitializer = dataInitializer;
        this.webSocketInitializer = webSocketInitializer;
        this.referencePriceStore = referencePriceStore;
    }

    public boolean isInitialized() {
        return dataInitializer.isInitialized() &&
                webSocketInitializer.isVegaWebSocketsInitialized() &&
                referencePriceStore.get().isPresent() &&
                (webSocketInitializer.isPolygonWebSocketInitialized() || webSocketInitializer.isBinanceWebSocketInitialized());
    }

    /**
     * Execute trading task on a given market
     *
     * @param marketConfig {@link MarketConfig}
     */
    public abstract void execute(MarketConfig marketConfig);
}