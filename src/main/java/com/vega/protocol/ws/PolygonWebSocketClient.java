package com.vega.protocol.ws;

import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

@Slf4j
public class PolygonWebSocketClient extends WebSocketClient {

    private final String symbol;
    private final ReferencePriceStore referencePriceStore;

    public PolygonWebSocketClient(URI uri,
                                  String symbol,
                                  ReferencePriceStore referencePriceStore) {
        super(uri);
        this.symbol = symbol;
        this.referencePriceStore = referencePriceStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        // TODO - implement streaming here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(String message) {
        log.info(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error(reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception e) {
        log.error(e.getMessage(), e);
    }
}
