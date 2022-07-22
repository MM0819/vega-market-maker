package com.vega.protocol.ws;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;

@Slf4j
public class VegaWebSocketClient extends WebSocketClient {

    private static final String MARKETS_QUERY =
    """
        subscription {
            marketsData {
                market {
                    id
                    name
                    decimalPlaces
                    tradingMode
                    state
                }
            }
        }
    """;

    public VegaWebSocketClient(URI uri) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        try {
            JSONObject init = new JSONObject()
                    .put("type", "connection_init");
            this.send(init.toString());
            JSONObject marketsQuery = new JSONObject()
                    .put("query", MARKETS_QUERY);
            JSONObject marketsSubscription = new JSONObject()
                    .put("id", "markets")
                    .put("type", "start")
                    .put("payload", marketsQuery);
            this.send(marketsSubscription.toString());
        } catch(Exception e) {
            log.error(e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }

    @Override
    public void onMessage(String message) {
        log.info(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error(reason);
    }

    @Override
    public void onError(Exception e) {
        log.error(e.getMessage());
        log.debug(e.getMessage(), e);
    }
}