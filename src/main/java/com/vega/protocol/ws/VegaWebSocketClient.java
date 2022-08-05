package com.vega.protocol.ws;

import com.vega.protocol.model.Market;
import com.vega.protocol.store.MarketStore;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.json.JSONArray;
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
                  	tradableInstrument {
    				    instrument {
                            product{
                                ...on Future {
                                    quoteName
                                }
                            }
                        }
                    }
                }
            }
        }
    """;

    private final MarketStore marketStore;

    /**
     * Create a websocket client for Vega
     *
     * @param uri the websocket URI
     */
    public VegaWebSocketClient(
            final MarketStore marketStore,
            final URI uri
    ) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
        this.marketStore = marketStore;
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
        try {
            log.info(message);
            JSONObject jsonObject = new JSONObject(message);
            String id = jsonObject.optString("id");
            if(id != null) {
                JSONObject data = jsonObject.getJSONObject("payload").getJSONObject("data");
                if (id.equals("markets")) {
                    handleMarkets(data.getJSONArray("marketsData"));
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleMarkets(JSONArray marketsArray) {
        for(int i=0; i<marketsArray.length(); i++) {
            try {
                JSONObject marketsObject = marketsArray.getJSONObject(i).getJSONObject("market");
                String id = marketsObject.getString("id");
                String name = marketsObject.getString("name");
                int decimalPlaces = marketsObject.getInt("decimalPlaces");
                String state = marketsObject.getString("state").toUpperCase();
                String quoteName = marketsObject
                        .getJSONObject("tradableInstrument")
                        .getJSONObject("instrument")
                        .getJSONObject("product")
                        .getString("quoteName");
                Market market = new Market()
                        .setId(id)
                        .setName(name)
                        .setStatus(state)
                        .setDecimalPlaces(decimalPlaces)
                        .setSettlementAsset(quoteName);
                marketStore.add(market);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
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