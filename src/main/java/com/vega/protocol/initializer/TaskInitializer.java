package com.vega.protocol.initializer;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.task.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

@Slf4j
@Component
public class TaskInitializer {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    private final Map<UUID, Map<Class<? extends TradingTask>, ScheduledFuture<?>>> scheduledTasks = new HashMap<>();

    private final MarketConfigRepository marketConfigRepository;
    private final HedgeExposureTask hedgeExposureTask;
    private final UpdateQuotesTask updateQuotesTask;
    private final UpdateLiquidityCommitmentTask updateLiquidityCommitmentTask;
    private final NaiveFlowTask naiveFlowTask;

    private final Map<Class<? extends TradingTask>, Consumer<MarketConfig>> tasks = new HashMap<>();

    @PostConstruct
    private void setup() {
        tasks.put(HedgeExposureTask.class, hedgeExposureTask::execute);
        tasks.put(UpdateQuotesTask.class, updateQuotesTask::execute);
        tasks.put(UpdateLiquidityCommitmentTask.class, updateLiquidityCommitmentTask::execute);
        tasks.put(NaiveFlowTask.class, naiveFlowTask::execute);
    }

    public TaskInitializer(MarketConfigRepository marketConfigRepository,
                           HedgeExposureTask hedgeExposureTask,
                           UpdateQuotesTask updateQuotesTask,
                           UpdateLiquidityCommitmentTask updateLiquidityCommitmentTask,
                           NaiveFlowTask naiveFlowTask) {
        this.marketConfigRepository = marketConfigRepository;
        this.hedgeExposureTask = hedgeExposureTask;
        this.updateQuotesTask = updateQuotesTask;
        this.updateLiquidityCommitmentTask = updateLiquidityCommitmentTask;
        this.naiveFlowTask = naiveFlowTask;
    }

    /**
     * Initialize the scheduled tasks if there are any present in the database
     */
    public void initialize() {
        marketConfigRepository.findAll().forEach(marketConfig -> {
            scheduleTask(marketConfig.getUpdateHedgeEnabled(), marketConfig.getUpdateHedgeFrequency(),
                    marketConfig, HedgeExposureTask.class);
            scheduleTask(marketConfig.getUpdateQuotesEnabled(), marketConfig.getUpdateQuotesFrequency(),
                    marketConfig, UpdateQuotesTask.class);
            scheduleTask(marketConfig.getUpdateLiquidityCommitmentEnabled(), marketConfig.getUpdateLiquidityCommitmentFrequency(),
                    marketConfig, UpdateLiquidityCommitmentTask.class);
            scheduleTask(marketConfig.getUpdateNaiveFlowEnabled(), marketConfig.getUpdateNaiveFlowFrequency(),
                    marketConfig, NaiveFlowTask.class);
        });
    }

    /**
     * Create a scheduled {@link TradingTask}
     *
     * @param enabled true if the task is enabled
     * @param frequency the execution frequency (in seconds)
     * @param marketConfig {@link MarketConfig} for the task
     * @param type the type of {@link TradingTask}
     */
    public void scheduleTask(
            final boolean enabled,
            final int frequency,
            final MarketConfig marketConfig,
            final Class<? extends TradingTask> type
    ) {
        if(enabled) {
            cancelTask(marketConfig.getId(), type);
            ScheduledFuture<?> future = scheduler.schedule(
                    () -> tasks.get(type).accept(marketConfig), new CronTrigger(getCronFromSeconds(frequency)));
            scheduledTasks.computeIfAbsent(marketConfig.getId(), k -> new HashMap<>());
            scheduledTasks.get(marketConfig.getId()).put(type, future);
        }
    }

    /**
     * Cancel a scheduled {@link TradingTask}
     *
     * @param id {@link MarketConfig} ID
     * @param type the type of {@link TradingTask}
     */
    public void cancelTask(
            final UUID id,
            final Class<? extends TradingTask> type
    ) {
        scheduledTasks.computeIfAbsent(id, k -> new HashMap<>());
        ScheduledFuture<?> future = scheduledTasks.get(id).get(type);
        if(future != null) {
            future.cancel(false);
            scheduledTasks.get(id).remove(type);
            if(scheduledTasks.get(id).size() == 0) {
                scheduledTasks.remove(id);
            }
        }
    }

    /**
     * Get cron expression from seconds
     *
     * @param seconds the number of seconds
     *
     * @return the cron expression
     */
    private String getCronFromSeconds(
            final int seconds
    ) {
        if(seconds < 2) {
            log.info("Once per second");
            return "* * * * * *";
        } else if(seconds < 60) {
            log.info("Every {} seconds", seconds);
            return String.format("*/%s * * * * * ", seconds);
        } else if(seconds < 3600) {
            int minutes = (int) Math.ceil((double) seconds / 60.0);
            log.info("Every {} minutes", minutes);
            return String.format("0 */%s * * * * ", minutes);
        } else if(seconds < 86400) {
            int hours = (int) Math.ceil((double) seconds / 3600.0);
            log.info("Every {} hours", hours);
            return String.format("0 0 */%s * * * ", hours);
        }
        log.info("Once per hour");
        return "0 0 * * * *";
    }
}