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
            marketsData(marketIds: "MARKET_ID") {
            	marketId
            	marketState
            	marketTradingMode
            	bestBidPrice
            	bestOfferPrice
            	bestBidVolume
            	bestOfferVolume
            	markPrice
            	targetStake
            	suppliedStake
            	openInterest
            	priceMonitoringBounds {
            	    minValidPrice
            	    maxValidPrice
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
                    .put("query", MARKETS_QUERY
                            .replace("MARKET_ID", marketId));
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
                String assetId = accountObject.getString("assetId");
                Asset asset = assetStore.getById(assetId).orElseThrow(() ->
                        new TradingException(ErrorCode.ASSET_NOT_FOUND));
                int decimals = asset.getDecimalPlaces();
                String marketId = accountObject.optString("marketId");
                AccountType type = AccountType.valueOf(accountObject.getString("type")
                        .replace("ACCOUNT_TYPE_", ""));
                String id = String.format("%s-%s-%s", asset.getSymbol(), partyId, type);
                if(!StringUtils.hasText(marketId) && !type.equals(AccountType.GENERAL)) {
                    id = String.format("%s-%s", id, marketId);
                }
                BigDecimal balance = BigDecimal.valueOf(accountObject.getDouble("balance"));
                Account account = new Account()
                        .setAsset(asset.getSymbol())
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
                String marketId = positionObject.getString("marketId");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                BigDecimal size = BigDecimal.valueOf(positionObject.getDouble("openVolume"));
                BigDecimal unrealisedPnl = BigDecimal.valueOf(positionObject.getDouble("unrealisedPNL"));
                BigDecimal realisedPnl = BigDecimal.valueOf(positionObject.getDouble("realisedPNL"));
                BigDecimal entryPrice = BigDecimal.valueOf(positionObject.getDouble("averageEntryPrice"));
                Position position = new Position()
                        .setPartyId(partyId)
                        .setUnrealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), unrealisedPnl))
                        .setRealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), realisedPnl))
                        .setEntryPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), entryPrice))
                        .setMarket(market)
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), size.abs()))
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
                JSONObject orderObject = ordersArray.getJSONObject(i);
                String id = orderObject.getString("id");
                MarketSide side = MarketSide.valueOf(orderObject.getString("side")
                        .replace("SIDE_", ""));
                BigDecimal size = BigDecimal.valueOf(orderObject.getDouble("size"));
                BigDecimal remainingSize = BigDecimal.valueOf(orderObject.getDouble("remaining"));
                BigDecimal price = BigDecimal.valueOf(orderObject.getDouble("price"));
                String marketId = orderObject.getString("marketId");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                OrderType type = OrderType.valueOf(orderObject.getString("type")
                        .replace("TYPE_", ""));
                OrderStatus status = OrderStatus.valueOf(orderObject.getString("status")
                        .replace("STATUS_", ""));
                Order order = new Order()
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), size))
                        .setPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), price))
                        .setType(type)
                        .setStatus(status)
                        .setRemainingSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), remainingSize))
                        .setId(id)
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setSide(side)
                        .setIsPeggedOrder(orderObject.has("liquidityProvisionId") &&
                                orderObject.getString("liquidityProvisionId").length() > 0);
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
                JSONObject liquidityCommitmentObject = liquidityCommitmentsArray.getJSONObject(i);
                String id = liquidityCommitmentObject.getString("id");
                BigDecimal commitmentAmount = BigDecimal.valueOf(
                        liquidityCommitmentObject.getDouble("commitmentAmount"));
                BigDecimal fee = BigDecimal.valueOf(liquidityCommitmentObject.getDouble("fee"));
                LiquidityCommitmentStatus status = LiquidityCommitmentStatus.valueOf(liquidityCommitmentObject
                        .getString("status").replace("STATUS_", ""));
                JSONArray buysArray = liquidityCommitmentObject.getJSONArray("buys");
                JSONArray sellsArray = liquidityCommitmentObject.getJSONArray("sells");
                String marketId = liquidityCommitmentObject.getString("marketId");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                List<LiquidityCommitmentOffset> bids = orderService.parseLiquidityOrders(
                        buysArray, market.getDecimalPlaces());
                List<LiquidityCommitmentOffset> asks = orderService.parseLiquidityOrders(
                        sellsArray, market.getDecimalPlaces());
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
                JSONObject marketObject = marketsArray.getJSONObject(i);
                String id = marketObject.getString("marketId");
                MarketState state = MarketState.valueOf(marketObject.getString("marketState")
                        .replace("STATE_", ""));
                MarketTradingMode tradingMode = MarketTradingMode.valueOf(marketObject.getString("marketTradingMode")
                        .replace("TRADING_MODE_", ""));
                BigDecimal markPrice = BigDecimal.valueOf(marketObject.getLong("markPrice"));
                BigDecimal bestBidPrice = BigDecimal.valueOf(marketObject.getLong("bestBidPrice"));
                BigDecimal bestAskPrice = BigDecimal.valueOf(marketObject.getLong("bestOfferPrice"));
                BigDecimal bestBidSize = BigDecimal.valueOf(marketObject.getLong("bestBidVolume"));
                BigDecimal bestAskSize = BigDecimal.valueOf(marketObject.getLong("bestOfferVolume"));
                BigDecimal targetStake = BigDecimal.valueOf(marketObject.getLong("targetStake"));
                BigDecimal suppliedStake = BigDecimal.valueOf(marketObject.getLong("suppliedStake"));
                BigDecimal openInterest = BigDecimal.valueOf(marketObject.getLong("openInterest"));
                JSONArray priceMonitoringBounds = marketObject.getJSONArray("priceMonitoringBounds");
                BigDecimal minValidPrice = BigDecimal.ZERO;
                BigDecimal maxValidPrice = BigDecimal.valueOf(Double.MAX_VALUE);
                for(int j=0; j<priceMonitoringBounds.length(); j++) {
                    JSONObject bound = priceMonitoringBounds.getJSONObject(j);
                    BigDecimal min = BigDecimal.valueOf(bound.getLong("minValidPrice"));
                    BigDecimal max = BigDecimal.valueOf(bound.getLong("maxValidPrice"));
                    if(min.doubleValue() > minValidPrice.doubleValue()) {
                        minValidPrice = min;
                    }
                    if(max.doubleValue() < maxValidPrice.doubleValue()) {
                        maxValidPrice = max;
                    }
                }
                BigDecimal finalMinValidPrice = minValidPrice;
                BigDecimal finalMaxValidPrice = maxValidPrice;
                marketStore.getById(id).ifPresent(market -> {
                    Asset asset = assetStore.getItems().stream()
                            .filter(a -> a.getSymbol().equals(market.getSettlementAsset())).findFirst()
                            .orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
                    int positionDecimals = market.getPositionDecimalPlaces();
                    int marketDecimals = market.getDecimalPlaces();
                    int assetDecimals = asset.getDecimalPlaces();
                    marketStore.update(market
                            .setState(state)
                            .setTradingMode(tradingMode)
                            .setMinValidPrice(decimalUtils.convertToDecimals(marketDecimals, finalMinValidPrice))
                            .setMaxValidPrice(decimalUtils.convertToDecimals(marketDecimals, finalMaxValidPrice))
                            .setTargetStake(decimalUtils.convertToDecimals(assetDecimals, targetStake))
                            .setSuppliedStake(decimalUtils.convertToDecimals(assetDecimals, suppliedStake))
                            .setMarkPrice(decimalUtils.convertToDecimals(marketDecimals, markPrice))
                            .setBestAskPrice(decimalUtils.convertToDecimals(marketDecimals, bestAskPrice))
                            .setBestBidPrice(decimalUtils.convertToDecimals(marketDecimals, bestBidPrice))
                            .setBestAskSize(decimalUtils.convertToDecimals(positionDecimals, bestAskSize))
                            .setBestBidSize(decimalUtils.convertToDecimals(positionDecimals, bestBidSize))
                            .setOpenInterest(decimalUtils.convertToDecimals(positionDecimals, openInterest)));
                });
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
            array.put(data.optJSONObject(key));
        }
        return array;
    }
}