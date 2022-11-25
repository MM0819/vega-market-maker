package com.vega.protocol.initializer;

import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.ws.BinanceWebSocketClient;
import com.vega.protocol.ws.PolygonWebSocketClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class WebSocketInitializer {

    private final String binanceWsUrl;
    private final String polygonWsUrl;
    private final Boolean binanceWsEnabled;
    private final Boolean polygonWsEnabled;
    private final ReferencePriceStore referencePriceStore;

    public WebSocketInitializer(@Value("${binance.ws.url}") String binanceWsUrl,
                                @Value("${polygon.ws.url}") String polygonWsUrl,
                                @Value("${binance.ws.enabled}") Boolean binanceWsEnabled,
                                @Value("${polygon.ws.enabled}") Boolean polygonWsEnabled,
                                ReferencePriceStore referencePriceStore) {
        this.binanceWsUrl = binanceWsUrl;
        this.polygonWsUrl = polygonWsUrl;
        this.binanceWsEnabled = binanceWsEnabled;
        this.polygonWsEnabled = polygonWsEnabled;
        this.referencePriceStore = referencePriceStore;
    }

    @Getter
    private BinanceWebSocketClient binanceWebSocketClient;
    @Getter
    private PolygonWebSocketClient polygonWebSocketClient;
    @Getter
    private boolean binanceWebSocketInitialized = false;
    @Getter
    private boolean polygonWebSocketInitialized = false;

    public void initialize() {
        if(binanceWsEnabled) {
            initializeBinance();
        }
        if(polygonWsEnabled) {
            initializePolygon();
        }
    }

    private void initializeBinance() {
        log.info("Connecting to Binance Web Socket...");
        binanceWebSocketClient = new BinanceWebSocketClient(URI.create(binanceWsUrl), referencePriceStore);
        binanceWebSocketClient.connect();
        log.info("Connected to {}", binanceWebSocketClient.getURI().toString());
        binanceWebSocketInitialized = true;
    }

    private void initializePolygon() {
        log.info("Connecting to Polygon Web Socket...");
        polygonWebSocketClient = new PolygonWebSocketClient(URI.create(polygonWsUrl), referencePriceStore);
        polygonWebSocketClient.connect();
        log.info("Connected to {}", polygonWebSocketClient.getURI().toString());
        polygonWebSocketInitialized = true;
    }

    @Scheduled(cron = "* * * * * *")
    public void keepWebSocketsAlive() {
        if(binanceWebSocketInitialized) {
            if (binanceWebSocketClient.isClosed()) {
                binanceWebSocketClient.reconnect();
            }
        }
        if(polygonWebSocketInitialized) {
            if (polygonWebSocketClient.isClosed()) {
                polygonWebSocketClient.reconnect();
            }
        }
    }
}