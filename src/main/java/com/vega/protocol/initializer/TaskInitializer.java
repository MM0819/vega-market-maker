package com.vega.protocol.initializer;

import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.task.HedgeExposureTask;
import com.vega.protocol.task.NaiveFlowTask;
import com.vega.protocol.task.UpdateLiquidityCommitmentTask;
import com.vega.protocol.task.UpdateQuotesTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

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
            if(marketConfig.getUpdateHedgeEnabled()) {
                String expression = String.format("*/%s * * * * *", marketConfig.getUpdateHedgeFrequency()); // TODO - create utils to parse this properly (e.g. to make it work for seconds > 59 and < 2
                CronTrigger cronTrigger = new CronTrigger(expression);
                scheduler.schedule(() -> hedgeExposureTask.execute(marketConfig), cronTrigger);
            }
            if(marketConfig.getUpdateLiquidityCommitmentEnabled()) {
                String expression = String.format("*/%s * * * * *", marketConfig.getUpdateLiquidityCommitmentFrequency()); // TODO - create utils to parse this properly (e.g. to make it work for seconds > 59 and < 2
                CronTrigger cronTrigger = new CronTrigger(expression);
                scheduler.schedule(() -> updateLiquidityCommitmentTask.execute(marketConfig), cronTrigger);
            }
            if(marketConfig.getUpdateQuotesEnabled()) {
                String expression = String.format("*/%s * * * * *", marketConfig.getUpdateQuotesFrequency()); // TODO - create utils to parse this properly (e.g. to make it work for seconds > 59 and < 2
                CronTrigger cronTrigger = new CronTrigger(expression);
                scheduler.schedule(() -> updateQuotesTask.execute(marketConfig), cronTrigger);
            }
            if(marketConfig.getNaiveFlowEnabled()) {
                String expression = String.format("*/%s * * * * *", marketConfig.getUpdateNaiveFlowFrequency()); // TODO - create utils to parse this properly (e.g. to make it work for seconds > 59 and < 2
                CronTrigger cronTrigger = new CronTrigger(expression);
                scheduler.schedule(() -> naiveFlowTask.execute(marketConfig), cronTrigger);
            }
        });
    }
}