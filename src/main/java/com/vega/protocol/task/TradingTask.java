package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;

public abstract class TradingTask {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    protected final DataInitializer dataInitializer;

    protected TradingTask(DataInitializer dataInitializer) {
        this.dataInitializer = dataInitializer;
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