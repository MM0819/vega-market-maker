package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class TradingTask {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    protected final DataInitializer dataInitializer;
    protected final WebSocketInitializer webSocketInitializer;
    protected final ReferencePriceStore referencePriceStore;
    protected final boolean taskEnabled;

    protected TradingTask(DataInitializer dataInitializer,
                          WebSocketInitializer webSocketInitializer,
                          ReferencePriceStore referencePriceStore,
                          boolean taskEnabled) {
        this.dataInitializer = dataInitializer;
        this.webSocketInitializer = webSocketInitializer;
        this.referencePriceStore = referencePriceStore;
        this.taskEnabled = taskEnabled;
    }

    public boolean isInitialized() {
        return dataInitializer.isInitialized() &&
                webSocketInitializer.isVegaWebSocketsInitialized() &&
                referencePriceStore.get().isPresent() &&
                (webSocketInitializer.isPolygonWebSocketInitialized() || webSocketInitializer.isBinanceWebSocketInitialized());
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