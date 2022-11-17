package com.vega.protocol.initializer;

import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.task.HedgeExposureTask;
import com.vega.protocol.task.UpdateQuotesTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class TaskInitializer {

    private static final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    private final MarketConfigRepository marketConfigRepository;
    private final HedgeExposureTask hedgeExposureTask;
    private final UpdateQuotesTask updateQuotesTask;

    public TaskInitializer(MarketConfigRepository marketConfigRepository,
                           HedgeExposureTask hedgeExposureTask,
                           UpdateQuotesTask updateQuotesTask) {
        this.marketConfigRepository = marketConfigRepository;
        this.hedgeExposureTask = hedgeExposureTask;
        this.updateQuotesTask = updateQuotesTask;
    }

    @PostConstruct
    private void initialize() {
        marketConfigRepository.findAll().forEach(marketConfig -> {
            if(marketConfig.getUpdateHedgeEnabled()) {
                String expression = String.format("*/%s * * * * *", marketConfig.getUpdateHedgeFrequency()); // TODO - create utils to parse this properly (e.g. to make it work for seconds > 59 and < 2
                CronTrigger cronTrigger = new CronTrigger(expression);
                scheduler.schedule(() -> hedgeExposureTask.execute(marketConfig), cronTrigger);
            }
            if(marketConfig.getUpdateLiquidityCommitmentEnabled()) {

            }
            if(marketConfig.getUpdateQuotesEnabled()) {

            }
        });
    }
}