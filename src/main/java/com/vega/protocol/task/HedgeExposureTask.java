package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    public HedgeExposureTask(DataInitializer dataInitializer,
                             WebSocketInitializer webSocketInitializer) {
        super(dataInitializer, webSocketInitializer);
    }

    @Override
    public String getCronExpression() {
        return "0 */10 * * * *";
    }

    @Override
    public void execute() {
        if(!isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        // TODO - implement hedging on Binance or IG (for non-crypto)
        log.info("Hedging exposure...");
    }
}