package com.vega.protocol;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.TaskInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class Application implements CommandLineRunner {

    private final WebSocketInitializer webSocketInitializer;
    private final DataInitializer dataInitializer;
    private final TaskInitializer taskInitializer;
    private final SleepUtils sleepUtils;

    public Application(WebSocketInitializer webSocketInitializer,
                       DataInitializer dataInitializer,
                       TaskInitializer taskInitializer,
                       SleepUtils sleepUtils) {
        this.webSocketInitializer = webSocketInitializer;
        this.dataInitializer = dataInitializer;
        this.taskInitializer = taskInitializer;
        this.sleepUtils = sleepUtils;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting market maker...");
        dataInitializer.initialize();
        sleepUtils.sleep(5000L);
        webSocketInitializer.initialize();
        sleepUtils.sleep(5000L);
        taskInitializer.initialize();
    }
}
