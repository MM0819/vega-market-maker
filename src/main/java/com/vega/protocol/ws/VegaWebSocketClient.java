package com.vega.protocol.ws;

import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.Account;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.model.Position;
import com.vega.protocol.store.AccountStore;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.PositionStore;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;

@Slf4j
public class VegaWebSocketClient extends WebSocketClient {

    // TODO - subscribe to asset updates
    private static final String ASSETS_QUERY = """
    """;

    private static final String ACCOUNTS_QUERY = """
        subscription {
            accounts(partyId: "PARTY_ID") {
                balance
                asset {
                    symbol
                    decimals
                    name
                    id
                }
                market {
                    id
                    name
                    tradableInstrument {
                        instrument {
                            product {
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

    private static final String ORDERS_QUERY = """
        subscription {
            orders(partyId: "PARTY_ID", marketId: "MARKET_ID") {
                id
                price
                side
                type
                size
                remaining
                status
                market {
                    id
                }
                liquidityProvision {
                    commitmentAmount
                    fee
                    sells {
                        liquidityOrder {
                            reference
                            proportion
                            offset
                        }
                    }
                    buys {
                        liquidityOrder {
                            reference
                            proportion
                            offset
                        }
                    }
                }
            }
        }
    """;

    private static final String POSITIONS_QUERY =
    """
        subscription {
            positions(partyId: "PARTY_ID") {
                openVolume
                realisedPNL
                unrealisedPNL
                averageEntryPrice
                market {
                    id
                }
            }
        }
    """;

    private static final String MARKETS_QUERY =
    """
        subscription {
            marketsData {
                market {
                    id
                    name
                    decimalPlaces
                    positionDecimalPlaces
                    tradingMode
                    state
                  	tradableInstrument {
    				    instrument {
                            product {
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
    private final PositionStore positionStore;
    private final AccountStore accountStore;
    private final String partyId;
    private final String marketId;
    private final DecimalUtils decimalUtils;

    /**
     * Create a websocket client for Vega
     *
     * @param partyId the Vega party ID
     * @param marketId the Vega market ID
     * @param marketStore {@link MarketStore}
     * @param orderStore {@link OrderStore}
     * @param positionStore {@link PositionStore}
     * @param accountStore {@link AccountStore}
     * @param decimalUtils {@link DecimalUtils}
     * @param uri the websocket URI
     */
    public VegaWebSocketClient(
            final String partyId,
            final String marketId,
            final MarketStore marketStore,
            final OrderStore orderStore,
            final PositionStore positionStore,
            final AccountStore accountStore,
            final DecimalUtils decimalUtils,
            final URI uri
    ) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
        this.marketStore = marketStore;
        this.orderStore = orderStore;
        this.positionStore = positionStore;
        this.accountStore = accountStore;
        this.decimalUtils = decimalUtils;
        this.partyId = partyId;
        this.marketId = marketId;
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
                    .put("query", ORDERS_QUERY
                            .replace("PARTY_ID", partyId)
                            .replace("MARKET_ID", marketId));
            JSONObject ordersSubscription = new JSONObject()
                    .put("id", "orders")
                    .put("type", "start")
                    .put("payload", ordersQuery);
            this.send(ordersSubscription.toString());
            JSONObject positionsQuery = new JSONObject()
                    .put("query", POSITIONS_QUERY
                            .replace("PARTY_ID", partyId));
            JSONObject positionsSubscription = new JSONObject()
                    .put("id", "positions")
                    .put("type", "start")
                    .put("payload", positionsQuery);
            this.send(positionsSubscription.toString());
            JSONObject accountsQuery = new JSONObject()
                    .put("query", ACCOUNTS_QUERY
                            .replace("PARTY_ID", partyId));
            JSONObject accountsSubscription = new JSONObject()
                    .put("id", "accounts")
                    .put("type", "start")
                    .put("payload", accountsQuery);
            this.send(accountsSubscription.toString());
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
                switch (id) {
                    case "markets" -> handleMarkets(data);
                    case "orders" -> handleOrders(data);
                    case "positions" -> handlePositions(data);
                    case "accounts" -> handleAccounts(data);
                    default -> log.warn("Unsupported message");
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Handle JSON array of accounts
     *
     * @param data {@link JSONObject}
     */
    private void handleAccounts(JSONObject data) throws JSONException {
        JSONArray accountsArray = getDataAsArray(data, "accounts");
        for(int i=0; i<accountsArray.length(); i++) {
            try {
                JSONObject accountObject = accountsArray.getJSONObject(i);
                String asset = accountObject.getJSONObject("asset").getString("symbol");
                int decimals = accountObject.getJSONObject("asset").getInt("decimals");
                JSONObject marketObject = accountObject.optJSONObject("market");
                AccountType type = AccountType.valueOf(accountObject.getString("type").toUpperCase());
                String id = String.format("%s-%s-%s", asset, partyId, type);
                if(marketObject != null) {
                    String marketId = marketObject.getString("id");
                    id = String.format("%s-%s", id, marketId);
                }
                BigDecimal balance = BigDecimal.valueOf(accountObject.getDouble("balance"));
                Account account = new Account()
                        .setAsset(asset)
                        .setType(type)
                        .setBalance(decimalUtils.convertToDecimals(decimals, balance))
                        .setPartyId(partyId)
                        .setId(id);
                accountStore.update(account);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of positions
     *
     * @param data {@link JSONObject}
     */
    private void handlePositions(JSONObject data) throws JSONException {
        JSONArray positionsArray = getDataAsArray(data, "positions");
        for(int i=0; i<positionsArray.length(); i++) {
            try {
                JSONObject positionObject = positionsArray.getJSONObject(i);
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                BigDecimal size = BigDecimal.valueOf(positionObject.getDouble("openVolume"));
                BigDecimal unrealisedPnl = BigDecimal.valueOf(positionObject.getDouble("unrealisedPNL"));
                BigDecimal realisedPnl = BigDecimal.valueOf(positionObject.getDouble("realisedPNL"));
                BigDecimal entryPrice = BigDecimal.valueOf(positionObject.getDouble("averageEntryPrice"));
                String marketId = positionObject.getJSONObject("market").getString("id");
                Position position = new Position()
                        .setPartyId(partyId)
                        .setUnrealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), unrealisedPnl))
                        .setRealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), realisedPnl))
                        .setEntryPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), entryPrice))
                        .setMarket(market)
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), entryPrice))
                        .setId(String.format("%s-%s", marketId, partyId))
                        .setSide(size.doubleValue() > 0 ? MarketSide.BUY :
                                (size.doubleValue() < 0 ? MarketSide.SELL : null));
                positionStore.update(position);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of orders
     *
     * @param data {@link JSONObject}
     */
    private void handleOrders(JSONObject data) throws JSONException {
        JSONArray ordersArray = getDataAsArray(data, "orders");
        for(int i=0; i<ordersArray.length(); i++) {
            try {
                JSONObject ordersObject = ordersArray.getJSONObject(i);
                String id = ordersObject.getString("id");
                MarketSide side = MarketSide.valueOf(ordersObject.getString("side").toUpperCase());
                BigDecimal size = BigDecimal.valueOf(ordersObject.getDouble("size"));
                BigDecimal remainingSize = BigDecimal.valueOf(ordersObject.getDouble("remaining"));
                BigDecimal price = BigDecimal.valueOf(ordersObject.getDouble("price"));
                String marketId = ordersObject.getJSONObject("market").getString("id");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                OrderType type = OrderType.valueOf(ordersObject.getString("type").toUpperCase());
                OrderStatus status = OrderStatus.valueOf(ordersObject.getString("status").toUpperCase());
                Order order = new Order()
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), size))
                        .setPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), price))
                        .setType(type)
                        .setStatus(status)
                        .setRemainingSize(remainingSize)
                        .setId(id)
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setSide(side);
                orderStore.update(order);
                // TODO - LP commitment is also in this stream
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of markets
     *
     * @param data {@link JSONObject}
     */
    private void handleMarkets(JSONObject data) throws JSONException {
        JSONArray marketsArray = getDataAsArray(data, "marketsData");
        for(int i=0; i<marketsArray.length(); i++) {
            try {
                JSONObject marketObject = marketsArray.getJSONObject(i).getJSONObject("market");
                String id = marketObject.getString("id");
                String name = marketObject.getString("name");
                int decimalPlaces = marketObject.getInt("decimalPlaces");
                MarketState state = MarketState.valueOf(marketObject.getString("state")
                        .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());


                MarketTradingMode tradingMode = MarketTradingMode.valueOf(marketObject.getString("tradingMode")
                        .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());


                String quoteName = marketObject
                        .getJSONObject("tradableInstrument")
                        .getJSONObject("instrument")
                        .getJSONObject("product")
                        .getString("quoteName");
                int positionDecimalPlaces = marketObject.getInt("positionDecimalPlaces");
                Market market = new Market()
                        .setId(id)
                        .setName(name)
                        .setState(state)
                        .setTradingMode(tradingMode)
                        .setDecimalPlaces(decimalPlaces)
                        .setPositionDecimalPlaces(positionDecimalPlaces)
                        .setSettlementAsset(quoteName);
                marketStore.update(market);
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

    /**
     * Get data as {@link JSONArray}
     *
     * @param data {@link JSONObject}
     * @param key the data key
     *
     * @return {@link JSONArray}
     */
    private JSONArray getDataAsArray(
            final JSONObject data,
            final String key
    ) throws JSONException {
        JSONArray array = new JSONArray();
        try {
            array = data.getJSONArray(key);
        } catch(Exception e) {
            array.put(data.getJSONObject(key));
        }
        return array;
    }
}