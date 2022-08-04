package com.vega.protocol.utils;

import org.junit.jupiter.api.Test;

public class SleepUtilsTest {

    private final SleepUtils sleepUtils = new SleepUtils();

    @Test
    public void testSleepWithInterrupt() {
        Runnable task = () -> sleepUtils.sleep(5000L);
        final Thread thread = new Thread(task);
        thread.start();
        thread.interrupt();
        sleepUtils.sleep(1000L);
    }
}