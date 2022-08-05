package com.vega.protocol.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SleepUtils {

    /**
     * Sleep for given milliseconds
     *
     * @param millis sleep duration
     */
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}