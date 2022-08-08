package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;

public abstract class TradingTask {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    protected final DataInitializer dataInitializer;
    protected final WebSocketInitializer webSocketInitializer;

    protected TradingTask(DataInitializer dataInitializer,
                          WebSocketInitializer webSocketInitializer) {
        this.dataInitializer = dataInitializer;
        this.webSocketInitializer = webSocketInitializer;
    }

    public boolean isInitialized() {
        return dataInitializer.isInitialized() && webSocketInitializer.isVegaWebSocketsInitialized() &&
                (webSocketInitializer.isPolygonWebSocketInitialized() ||
                        webSocketInitializer.isBinanceWebSocketInitialized());
    }

    /**
     * Get the cron expression for this task
     *
     * @return cron expression
     */
    public abstract String getCronExpression();

    /**
     * Executes this task
     */
    public abstract void execute();

    /**
     * Initialize the scheduled task
     */
    @PostConstruct
    public void initialize() {
        scheduler.initialize();
        scheduler.schedule(this::execute, new CronTrigger(getCronExpression()));
    }
}