package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    public HedgeExposureTask(DataInitializer dataInitializer) {
        super(dataInitializer);
    }

    @Override
    public String getCronExpression() {
        return "0 */10 * * * *";
    }

    @Override
    public void execute() {
        if(!dataInitializer.isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        // TODO - implement hedging on Binance or IG (for non-crypto)
        log.info("Hedging exposure...");
    }
}