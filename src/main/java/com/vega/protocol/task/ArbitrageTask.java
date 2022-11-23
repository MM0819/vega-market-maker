package com.vega.protocol.task;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArbitrageTask extends TradingTask {

    public ArbitrageTask(DataInitializer dataInitializer,
                         WebSocketInitializer webSocketInitializer,
                         ReferencePriceStore referencePriceStore) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
    }

    @Override
    public void execute(MarketConfig marketConfig) {
        // TODO - capture arbitrage opportunities between Vega and the underlying market (e.g. Binance)
    }
}