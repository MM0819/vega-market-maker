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

    // TODO - subscribe to LP orders
    // TODO - subscribe to orders
    // TODO - subscribe to positions

    private static final String LP_ORDERS_QUERY = "";
    private static final String ORDERS_QUERY = "";
    private static final String POSITIONS_QUERY = "";

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

    /**
     * Create a websocket client for Vega
     *
     * @param uri the websocket URI
     */
    public VegaWebSocketClient(URI uri) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
    }

    /**
     * {@inheritDoc}
     */
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
        log.error(e.getMessage());
        log.debug(e.getMessage(), e);
    }
}