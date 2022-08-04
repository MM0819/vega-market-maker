package com.vega.protocol.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SleepUtilsTest {

    private final SleepUtils sleepUtils = new SleepUtils();

    @Test
    public void testSleepWithInterrupt() {
        Runnable task = () -> sleepUtils.sleep(5000L);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(task);
        executorService.shutdown();
    }
}