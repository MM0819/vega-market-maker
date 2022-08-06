package com.vega.protocol.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HedgeExposureTaskTest {

    private HedgeExposureTask hedgeExposureTask;

    @BeforeEach
    public void setup() {
        hedgeExposureTask = new HedgeExposureTask();
    }

    @Test
    public void testExecute() {
        hedgeExposureTask.execute();
    }

    @Test
    public void testGetCronExpression() {
        String cron = hedgeExposureTask.getCronExpression();
        Assertions.assertEquals(cron, "0 */10 * * * *");
    }
}