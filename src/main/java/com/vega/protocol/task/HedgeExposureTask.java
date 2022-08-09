package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    public HedgeExposureTask(DataInitializer dataInitializer,
                             WebSocketInitializer webSocketInitializer,
                             @Value("${hedge.exposure.enabled}") Boolean taskEnabled) {
        super(dataInitializer, webSocketInitializer, taskEnabled);
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
        if(!taskEnabled) {
            log.debug("Cannot execute {} because it is disabled", getClass().getSimpleName());
            return;
        }
        // TODO - implement hedging on Binance or IG (for non-crypto)
        log.info("Hedging exposure...");
    }
}