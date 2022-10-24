package com.vega.protocol.ws;

import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.*;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.*;
import com.vega.protocol.utils.DecimalUtils;
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
import java.util.List;

@Slf4j
public class VegaWebSocketClient extends WebSocketClient {

    private static final String ACCOUNTS_QUERY =
    """
        subscription {
            accounts(partyId: "PARTY_ID") {
                balance
                type
                assetId
                marketId
            }
        }
    """;

    private static final String ORDERS_QUERY =
    """
        subscription {
            orders(partyId: "PARTY_ID", marketId: "MARKET_ID") {
                id
                price
                side
                type
                size
                remaining
                status
                marketId
                liquidityProvisionId
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
                marketId
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
                                    settlementAsset {
                                        id
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    """;

    private static  final String LIQUIDITY_COMMITMENT_QUERY =
    """
        subscription {
            liquidityProvisions(partyId: "PARTY_ID") {
                id
                partyID
                marketID
                commitmentAmount
                fee
                buys {
                    order {
                        id
                    }
                    liquidityOrder {
                        reference
                        proportion
                        offset
                    }
                }
                sells {
                    order {
                        id
                    }
                    liquidityOrder {
                        reference
                        proportion
                        offset
                    }
                }
                status
                version
                reference
            }
        }
    """;

    private final MarketStore marketStore;
    private final OrderStore orderStore;
    private final PositionStore positionStore;
    private final AccountStore accountStore;
    private final AssetStore assetStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final String partyId;
    private final String marketId;
    private final DecimalUtils decimalUtils;
    private final OrderService orderService;

    /**
     * Create a websocket client for Vega
     *
     * @param partyId the Vega party ID
     * @param marketId the Vega market ID
     * @param marketStore {@link MarketStore}
     * @param orderStore {@link OrderStore}
     * @param positionStore {@link PositionStore}
     * @param accountStore {@link AccountStore}
     * @param assetStore {@link AssetStore}
     * @param liquidityCommitmentStore {@link LiquidityCommitmentStore}
     * @param decimalUtils {@link DecimalUtils}
     * @param orderService {@link OrderService}
     * @param uri the websocket URI
     */
    public VegaWebSocketClient(
            final String partyId,
            final String marketId,
            final MarketStore marketStore,
            final OrderStore orderStore,
            final PositionStore positionStore,
            final AccountStore accountStore,
            final AssetStore assetStore,
            final LiquidityCommitmentStore liquidityCommitmentStore,
            final DecimalUtils decimalUtils,
            final OrderService orderService,
            final URI uri
    ) {
        super(uri, new Draft_6455(Collections.emptyList(),
                Collections.singletonList(new Protocol("graphql-ws"))));
        this.marketStore = marketStore;
        this.orderStore = orderStore;
        this.positionStore = positionStore;
        this.accountStore = accountStore;
        this.assetStore = assetStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.decimalUtils = decimalUtils;
        this.orderService = orderService;
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
            JSONObject liquidityCommitmentQuery = new JSONObject()
                    .put("query", LIQUIDITY_COMMITMENT_QUERY
                            .replace("PARTY_ID", partyId));
            JSONObject liquidityCommitmentSubscription = new JSONObject()
                    .put("id", "liquidityCommitment")
                    .put("type", "start")
                    .put("payload", liquidityCommitmentQuery);
            this.send(liquidityCommitmentSubscription.toString());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
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
            JSONObject payload = jsonObject.optJSONObject("payload");
            if(StringUtils.hasText(id) && payload != null) {
                JSONObject data = payload.getJSONObject("data");
                switch (id) {
                    case "markets" -> handleMarkets(data);
                    case "orders" -> handleOrders(data);
                    case "positions" -> handlePositions(data);
                    case "accounts" -> handleAccounts(data);
                    case "liquidityCommitment" -> handleLiquidityCommitment(data);
                    default -> log.warn("Unsupported message");
                }
            }
        } catch(Exception e) {
            log.info(message);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Handle JSON array of accounts
     *
     * @param data {@link JSONObject}
     */
    private void handleAccounts(JSONObject data) {
        JSONArray accountsArray = getDataAsArray(data, "accounts");
        for(int i=0; i<accountsArray.length(); i++) {
            try {
                JSONObject accountObject = accountsArray.getJSONObject(i);
                String asset = accountObject.getJSONObject("asset").getString("symbol");
                int decimals = accountObject.getJSONObject("asset").getInt("decimals");
                JSONObject marketObject = accountObject.optJSONObject("market");
                AccountType type = AccountType.valueOf(accountObject.getString("type").toUpperCase());
                String id = String.format("%s-%s-%s", asset, partyId, type);
                if(marketObject != null && !type.equals(AccountType.GENERAL)) {
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
                log.info(data.toString());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of positions
     *
     * @param data {@link JSONObject}
     */
    private void handlePositions(JSONObject data) {
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
                log.info(data.toString());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of orders
     *
     * @param data {@link JSONObject}
     */
    private void handleOrders(JSONObject data) {
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
            } catch(Exception e) {
                log.info(data.toString());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON object for liquidity commitment
     *
     * @param data {@link JSONObject}
     */
    private void handleLiquidityCommitment(
            final JSONObject data
    ) {
        JSONArray liquidityCommitmentsArray = getDataAsArray(data, "liquidityProvisions");
        for(int i=0; i<liquidityCommitmentsArray.length(); i++) {
            try {
                JSONObject liquidityCommitmentObject = liquidityCommitmentsArray.getJSONObject(i)
                        .getJSONObject("liquidityProvision");
                String id = liquidityCommitmentObject.getString("id");
                BigDecimal commitmentAmount = BigDecimal.valueOf(liquidityCommitmentObject.getDouble("commitmentAmount"));
                BigDecimal fee = BigDecimal.valueOf(liquidityCommitmentObject.getDouble("fee"));
                LiquidityCommitmentStatus status = LiquidityCommitmentStatus.valueOf(liquidityCommitmentObject
                        .getString("status").replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
                JSONArray buysArray = liquidityCommitmentObject.getJSONArray("buys");
                JSONArray sellsArray = liquidityCommitmentObject.getJSONArray("sells");
                String marketId = liquidityCommitmentObject.getString("marketID");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                List<LiquidityCommitmentOffset> bids = orderService.parseLiquidityOrders(
                        buysArray, market.getDecimalPlaces(), true);
                List<LiquidityCommitmentOffset> asks = orderService.parseLiquidityOrders(
                        sellsArray, market.getDecimalPlaces(), true);
                LiquidityCommitment liquidityCommitment = new LiquidityCommitment()
                        .setCommitmentAmount(commitmentAmount)
                        .setFee(fee)
                        .setStatus(status)
                        .setId(id)
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setBids(bids)
                        .setAsks(asks);
                liquidityCommitmentStore.update(liquidityCommitment);
            } catch(Exception e) {
                log.info(data.toString());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle JSON array of markets
     *
     * @param data {@link JSONObject}
     */
    private void handleMarkets(JSONObject data) {
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
                String settlementAssetId = marketObject
                        .getJSONObject("tradableInstrument")
                        .getJSONObject("instrument")
                        .getJSONObject("product")
                        .getJSONObject("settlementAsset")
                        .getString("id");
                Asset asset = assetStore.getById(settlementAssetId)
                        .orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
                int positionDecimalPlaces = marketObject.getInt("positionDecimalPlaces");
                Market market = new Market()
                        .setId(id)
                        .setName(name)
                        .setState(state)
                        .setTradingMode(tradingMode)
                        .setDecimalPlaces(decimalPlaces)
                        .setPositionDecimalPlaces(positionDecimalPlaces)
                        .setSettlementAsset(asset.getSymbol());
                marketStore.update(market);
            } catch(Exception e) {
                log.info(data.toString());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error("Closed: {}, {}, {} !!", code, reason, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception e) {
        log.error(e.getMessage(), e);
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
    ) {
        JSONArray array = new JSONArray();
        try {
            array = data.getJSONArray(key);
        } catch(Exception e) {
            JSONObject obj = data.optJSONObject(key);
            array = obj != null ? array.put(obj) : null;
        }
        return array == null ? new JSONArray() : array;
    }
}