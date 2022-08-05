package com.vega.protocol.initializer;

import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.ws.BinanceWebSocketClient;
import com.vega.protocol.ws.PolygonWebSocketClient;
import com.vega.protocol.ws.VegaWebSocketClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class WebSocketInitializer {

    private final String vegaWsUrl;
    private final String binanceWsUrl;
    private final String polygonWsUrl;
    private final Boolean vegaWsEnabled;
    private final Boolean binanceWsEnabled;
    private final Boolean polygonWsEnabled;
    private final String referencePriceMarket;
    private final ReferencePriceSource referencePriceSource;
    private final ReferencePriceStore referencePriceStore;
    private final MarketStore marketStore;

    public WebSocketInitializer(@Value("${vega.ws.url}") String vegaWsUrl,
                                @Value("${binance.ws.url}") String binanceWsUrl,
                                @Value("${polygon.ws.url}") String polygonWsUrl,
                                @Value("${vega.ws.enabled}") Boolean vegaWsEnabled,
                                @Value("${binance.ws.enabled}") Boolean binanceWsEnabled,
                                @Value("${polygon.ws.enabled}") Boolean polygonWsEnabled,
                                @Value("${reference.price.market}") String referencePriceMarket,
                                @Value("${reference.price.source}") ReferencePriceSource referencePriceSource,
                                ReferencePriceStore referencePriceStore,
                                MarketStore marketStore) {
        this.vegaWsUrl = vegaWsUrl;
        this.binanceWsUrl = binanceWsUrl;
        this.polygonWsUrl = polygonWsUrl;
        this.vegaWsEnabled = vegaWsEnabled;
        this.binanceWsEnabled = binanceWsEnabled;
        this.polygonWsEnabled = polygonWsEnabled;
        this.referencePriceMarket = referencePriceMarket;
        this.referencePriceSource = referencePriceSource;
        this.referencePriceStore = referencePriceStore;
        this.marketStore = marketStore;
    }

    @Getter
    private VegaWebSocketClient vegaWebSocketClient;
    @Getter
    private BinanceWebSocketClient binanceWebSocketClient;
    @Getter
    private PolygonWebSocketClient polygonWebSocketClient;
    private boolean vegaWebSocketsInitialized = false;
    private boolean binanceWebSocketInitialized = false;
    private boolean polygonWebSocketInitialized = false;

    public void initialize() {
        if(vegaWsEnabled) {
            initializeVega();
        }
        if(binanceWsEnabled && referencePriceSource.equals(ReferencePriceSource.BINANCE)) {
            initializeBinance();
        }
        if(polygonWsEnabled && referencePriceSource.equals(ReferencePriceSource.POLYGON)) {
            initializePolygon();
        }
    }

    private void initializeVega() {
        log.info("Connecting to Vega Web Socket...");
        vegaWebSocketClient = new VegaWebSocketClient(marketStore, URI.create(vegaWsUrl));
        vegaWebSocketClient.connect();
        log.info("Connected to {}", vegaWebSocketClient.getURI().toString());
        vegaWebSocketsInitialized = true;
    }

    private void initializeBinance() {
        log.info("Connecting to Binance Web Socket...");
        binanceWebSocketClient = new BinanceWebSocketClient(
                URI.create(binanceWsUrl), referencePriceMarket, referencePriceStore);
        binanceWebSocketClient.connect();
        log.info("Connected to {}", binanceWebSocketClient.getURI().toString());
        binanceWebSocketInitialized = true;
    }

    private void initializePolygon() {
        log.info("Connecting to Polygon Web Socket...");
        polygonWebSocketClient = new PolygonWebSocketClient(
                URI.create(polygonWsUrl), referencePriceMarket, referencePriceStore);
        polygonWebSocketClient.connect();
        log.info("Connected to {}", polygonWebSocketClient.getURI().toString());
        polygonWebSocketInitialized = true;
    }

    @Scheduled(cron = "* * * * * *")
    public void keepWebSocketsAlive() {
        if(vegaWebSocketsInitialized) {
            if (vegaWebSocketClient.isClosed()) {
                vegaWebSocketClient.reconnect();
            }
        }
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