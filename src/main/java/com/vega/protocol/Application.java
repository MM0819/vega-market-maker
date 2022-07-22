package com.vega.protocol;

import com.vega.protocol.initializer.WebSocketInitializer;
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

    public Application(WebSocketInitializer webSocketInitializer) {
        this.webSocketInitializer = webSocketInitializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting simple market maker...");
        webSocketInitializer.initialize();
    }
}