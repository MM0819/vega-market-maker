package com.vega.protocol.initializer;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.task.HedgeExposureTask;
import com.vega.protocol.task.NaiveFlowTask;
import com.vega.protocol.task.UpdateLiquidityCommitmentTask;
import com.vega.protocol.task.UpdateQuotesTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
public class TaskInitializer {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    private final MarketConfigRepository marketConfigRepository;
    private final HedgeExposureTask hedgeExposureTask;
    private final UpdateQuotesTask updateQuotesTask;
    private final UpdateLiquidityCommitmentTask updateLiquidityCommitmentTask;
    private final NaiveFlowTask naiveFlowTask;

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

    public void initialize() {
        marketConfigRepository.findAll().forEach(marketConfig -> {
            scheduleTask(marketConfig.getUpdateHedgeEnabled(), marketConfig.getUpdateHedgeFrequency(),
                    hedgeExposureTask::execute, marketConfig);
            scheduleTask(marketConfig.getUpdateQuotesEnabled(), marketConfig.getUpdateQuotesFrequency(),
                    updateQuotesTask::execute, marketConfig);
            scheduleTask(marketConfig.getUpdateLiquidityCommitmentEnabled(), marketConfig.getUpdateLiquidityCommitmentFrequency(),
                    updateLiquidityCommitmentTask::execute, marketConfig);
            scheduleTask(marketConfig.getUpdateNaiveFlowEnabled(), marketConfig.getUpdateNaiveFlowFrequency(),
                    naiveFlowTask::execute, marketConfig);
        });
    }

    private void scheduleTask(
            final boolean enabled,
            final int frequency,
            final Consumer<MarketConfig> task,
            final MarketConfig marketConfig
            ) {
        if(enabled) {
            scheduler.schedule(() -> task.accept(marketConfig), new CronTrigger(getCronFromSeconds(frequency)));
        }
    }

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