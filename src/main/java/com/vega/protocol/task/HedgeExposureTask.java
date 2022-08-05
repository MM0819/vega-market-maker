package com.vega.protocol.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    @Override
    public String getCronExpression() {
        return "0 */10 * * * *";
    }

    @Override
    public void execute() {
        // TODO - implement hedging on Binance or IG (for non-crypto)
        log.info("Hedging exposure...");
    }
}