package com.vega.protocol.ws;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;

@Slf4j
public class VegaWebSocketClient extends WebSocketClient {

    // TODO - subscribe to LP orders
    // TODO - subscribe to orders
    // TODO - subscribe to positions

    private static final String LP_ORDERS_QUERY = "";

    private static final String ORDERS_QUERY = """
        subscription {
            orders(partyId: "PARTY_ID") {
                id
                price
                side
                type
                size
                status
                market {
                    id
                }
            }
        }
    """;

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
    private final OrderStore orderStore;
    private final String partyId;

    /**
     * Create a websocket client for Vega
     *
     * @param uri the websocket URI
     */
    public VegaWebSocketClient(
            final String partyId,
            final MarketStore marketStore,
            final OrderStore orderStore,
            final URI uri
    ) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
        this.marketStore = marketStore;
        this.orderStore = orderStore;
        this.partyId = partyId;
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
            JSONObject ordersQuery = new JSONObject()
                    .put("query", ORDERS_QUERY.replace("PARTY_ID", partyId));
            JSONObject ordersSubscription = new JSONObject()
                    .put("id", "orders")
                    .put("type", "start")
                    .put("payload", ordersQuery);
            this.send(ordersSubscription.toString());
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
            JSONObject jsonObject = new JSONObject(message);
            String id = jsonObject.optString("id");
            if(StringUtils.hasText(id)) {
                JSONObject data = jsonObject.getJSONObject("payload").getJSONObject("data");
                if (id.equals("markets")) {
                    handleMarkets(data.getJSONArray("marketsData"));
                } else if(id.equals("orders")) {
                    handleOrders(data.getJSONArray("orders"));
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Handle JSON array of orders
     *
     * @param ordersArray {@link JSONArray}
     */
    private void handleOrders(JSONArray ordersArray) {
        for(int i=0; i<ordersArray.length(); i++) {
            try {
                JSONObject ordersObject = ordersArray.getJSONObject(i);
                String id = ordersObject.getString("id");
                MarketSide side = MarketSide.valueOf(ordersObject.getString("side").toUpperCase());
                BigDecimal size = BigDecimal.valueOf(ordersObject.getDouble("size"));
                BigDecimal remainingSize = BigDecimal.valueOf(ordersObject.getDouble("remaining"));
                BigDecimal price = BigDecimal.valueOf(ordersObject.getDouble("price"));
                String marketId = ordersObject.getJSONObject("market").getString("id");
                Market market = marketStore.getById(marketId).orElse(null);
                OrderType type = OrderType.valueOf(ordersObject.getString("type").toUpperCase());
                OrderStatus status = OrderStatus.valueOf(ordersObject.getString("status").toUpperCase());
                Order order = new Order()
                        .setSize(size)
                        .setPrice(price)
                        .setType(type)
                        .setStatus(status)
                        .setRemainingSize(remainingSize)
                        .setId(id)
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setSide(side);
                orderStore.add(order);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of orders
     *
     * @param marketsArray {@link JSONArray}
     */
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