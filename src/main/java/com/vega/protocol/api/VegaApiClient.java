package com.vega.protocol.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.*;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class VegaApiClient {

    private final String walletUrl;
    private final String walletUser;
    private final String walletPassword;
    private final String nodeUrl;
    private final String marketId;
    private final MarketStore marketStore;
    private final DecimalUtils decimalUtils;

    public VegaApiClient(@Value("${vega.wallet.url}") String walletUrl,
                         @Value("${vega.wallet.user}") String walletUser,
                         @Value("${vega.wallet.password}") String walletPassword,
                         @Value("${vega.node.url}") String nodeUrl,
                         @Value("${vega.market.id}") String marketId,
                         MarketStore marketStore,
                         DecimalUtils decimalUtils) {
        this.walletUrl = walletUrl;
        this.walletUser = walletUser;
        this.walletPassword = walletPassword;
        this.nodeUrl = nodeUrl;
        this.marketId = marketId;
        this.marketStore = marketStore;
        this.decimalUtils = decimalUtils;
    }

    /**
     * Get asset by ID
     *
     * @param id the asset ID
     *
     * @return {@link Optional<Asset>}
     */
    public Optional<Asset> getAsset(
            final String id
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/assets/%s", nodeUrl, id))
                    .asJson();
            JSONObject assetObject = response.getBody().getObject().getJSONObject("asset");
            String symbol = assetObject.getJSONObject("details").getString("symbol");
            int quantum = assetObject.getJSONObject("details").getInt("quantum");
            int decimalPlaces = assetObject.getJSONObject("details").getInt("decimals");
            String name = assetObject.getJSONObject("details").getString("name");
            AssetStatus status = AssetStatus.valueOf(assetObject.getString("status")
                    .replace("STATUS_", ""));
            Asset asset = new Asset()
                    .setSymbol(symbol)
                    .setQuantum(quantum)
                    .setDecimalPlaces(decimalPlaces)
                    .setName(name)
                    .setId(id)
                    .setStatus(status);
            return Optional.of(asset);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Get accounts
     *
     * @param partyId the party ID
     *
     * @return {@link List<Account>}
     */
    public List<Account> getAccounts(
            final String partyId
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/parties/%s/accounts", nodeUrl, partyId))
                    .asJson();
            List<Account> accounts = new ArrayList<>();
            JSONArray accountsArray = response.getBody().getObject().getJSONArray("accounts");
            Map<String, Asset> assetsById = new HashMap<>();
            for(int i=0; i<accountsArray.length(); i++) {
                JSONObject accountObject = accountsArray.getJSONObject(i);
                String assetId = accountObject.getString("asset");
                Asset asset = assetsById.get(assetId);
                if(asset == null) {
                    asset = getAsset(assetId).orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
                    assetsById.put(assetId, asset);
                }
                String marketId = accountObject.getString("marketId");
                BigDecimal balance = BigDecimal.valueOf(accountObject.getDouble("balance"));
                AccountType type = AccountType.valueOf(accountObject.getString("type")
                        .replace("ACCOUNT_TYPE_", ""));
                String id = String.format("%s-%s-%s", asset, partyId, type);
                if(!StringUtils.isBlank(marketId) && !marketId.equals("!")) {
                    id = String.format("%s-%s", id, marketId);
                }
                Account account = new Account()
                        .setId(id)
                        .setBalance(decimalUtils.convertToDecimals(asset.getDecimalPlaces(), balance))
                        .setAsset(asset.getSymbol())
                        .setType(type)
                        .setPartyId(partyId);
                accounts.add(account);
            }
            return accounts;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get markets
     *
     * @return {@link List<Market>}
     */
    public List<Market> getMarkets() {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/markets", nodeUrl)).asJson();
            JSONArray marketsArray = response.getBody().getObject().getJSONArray("markets");
            List<Market> markets = new ArrayList<>();
            for(int i=0; i<marketsArray.length(); i++) {
                JSONObject marketObject = marketsArray.getJSONObject(i);
                JSONObject tradableInstrument = marketObject.getJSONObject("tradableInstrument");
                String name = tradableInstrument.getJSONObject("instrument").getString("name");
                String settlementAsset = tradableInstrument.getJSONObject("instrument")
                        .getJSONObject("future").getString("quoteName");
                int decimalPlaces = marketObject.getInt("decimalPlaces");
                MarketState state = MarketState.valueOf(marketObject.getString("state")
                        .replace("STATE_", ""));
                MarketTradingMode tradingMode = MarketTradingMode.valueOf(marketObject.getString("tradingMode")
                        .replace("TRADING_MODE_", ""));
                Market market = new Market()
                        .setName(name)
                        .setSettlementAsset(settlementAsset)
                        .setDecimalPlaces(decimalPlaces)
                        .setId(marketId)
                        .setState(state)
                        .setTradingMode(tradingMode);
                markets.add(market);
            }
            return markets;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get positions
     *
     * @param partyId the party ID
     *
     * @return {@link List<Position>}
     */
    public List<Position> getPositions(
            final String partyId
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/parties/%s/positions", nodeUrl, partyId))
                    .asJson();
            List<Position> positions = new ArrayList<>();
            JSONArray positionsArray = response.getBody().getObject().getJSONArray("positions");
            for(int i=0; i<positionsArray.length(); i++) {
                JSONObject positionObject = positionsArray.getJSONObject(i);
                String marketId = positionObject.getString("marketId");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                BigDecimal size = BigDecimal.valueOf(positionObject.getDouble("openVolume"));
                BigDecimal entryPrice = BigDecimal.valueOf(positionObject.getDouble("averageEntryPrice"));
                BigDecimal realisedPnl = BigDecimal.valueOf(positionObject.getDouble("realisedPnl"));
                BigDecimal unrealisedPnl = BigDecimal.valueOf(positionObject.getDouble("unrealisedPnl"));
                MarketSide side = size.doubleValue() > 0 ? MarketSide.BUY :
                        (size.doubleValue() < 0 ? MarketSide.SELL : null);
                String id = String.format("%s-%s", marketId, partyId);
                Position position = new Position()
                        .setMarket(market)
                        .setEntryPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), entryPrice))
                        .setRealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), realisedPnl))
                        .setUnrealisedPnl(decimalUtils.convertToDecimals(market.getDecimalPlaces(), unrealisedPnl))
                        .setSide(side)
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), size))
                        .setId(id)
                        .setPartyId(partyId);
                positions.add(position);
            }
            return positions;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get all open orders
     *
     * @param partyId the party ID
     *
     * @return {@link List<Order>}
     */
    public List<Order> getOpenOrders(
            final String partyId
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/parties/%s/orders", nodeUrl, partyId))
                    .asJson();
            List<Order> orders = new ArrayList<>();
            JSONArray ordersArray = response.getBody().getObject().getJSONArray("orders");
            for(int i=0; i<ordersArray.length(); i++) {
                JSONObject orderObject = ordersArray.getJSONObject(i);
                String marketId = orderObject.getString("marketId");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                OrderType type = OrderType.valueOf(orderObject.getString("type")
                        .replace("TYPE_", ""));
                BigDecimal size = BigDecimal.valueOf(orderObject.getDouble("size"));
                OrderStatus status = OrderStatus.valueOf(orderObject.getString("status")
                        .replace("STATUS_", ""));
                MarketSide side = MarketSide.valueOf(orderObject.getString("side")
                        .replace("SIDE_", ""));
                BigDecimal remaining = BigDecimal.valueOf(orderObject.getDouble("remaining"));
                BigDecimal price = BigDecimal.valueOf(orderObject.getDouble("price"));
                String id = orderObject.getString("id");
                Order order = new Order()
                        .setType(type)
                        .setSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), size))
                        .setStatus(status)
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setRemainingSize(decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(), remaining))
                        .setPrice(decimalUtils.convertToDecimals(market.getDecimalPlaces(), price))
                        .setSide(side)
                        .setId(id);
                if(order.getStatus().equals(OrderStatus.ACTIVE)) {
                    orders.add(order);
                }
            }
            return orders;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Cancel order with recursive retry
     *
     * @param id the order ID
     * @param partyId the party ID
     * @param attempt the attempt count
     *
     * @return {@link Optional<String>}
     */
    private Optional<String> cancelOrder(
            final String id,
            final String partyId,
            final int attempt
    ) {
        if(attempt >= 10) {
            return Optional.empty();
        }
        try {
            JSONObject orderCancellation = new JSONObject()
                    .put("marketId", marketId)
                    .put("orderId", id);
            JSONObject cancellation = new JSONObject()
                    .put("orderCancellation", orderCancellation)
                    .put("pubKey", partyId)
                    .put("propagate", true);
            String token = getToken().orElseThrow(() -> new TradingException(ErrorCode.GET_VEGA_TOKEN_FAILED));
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", String.format("Bearer %s", token));
            HttpResponse<JsonNode> response = Unirest.post(String.format("%s/api/v1/command/sync", walletUrl))
                    .headers(headers)
                    .body(cancellation)
                    .asJson();
            if(response.getBody().toString().contains("couldn't get last block height")) {
                return cancelOrder(id, partyId, attempt+1);
            }
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Create order with recursive retry
     *
     * @param order the order
     * @param partyId the party ID
     * @param attempt the attempt count
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> submitOrder(
            final Order order,
            final String partyId,
            final int attempt
    ) {
        if(attempt >= 10) {
            return Optional.empty();
        }
        try {
            String reference = String.format("%s-%s", partyId, UUID.randomUUID());
            Market market = order.getMarket();
            String price = decimalUtils.convertFromDecimals(
                    market.getDecimalPlaces(), order.getPrice()).toBigInteger().toString();
            String size = decimalUtils.convertFromDecimals(
                    market.getPositionDecimalPlaces(), order.getSize()).toBigInteger().toString();
            JSONObject orderSubmission = new JSONObject()
                    .put("marketId", market.getId())
                    .put("price", price)
                    .put("size", size)
                    .put("side", String.format("SIDE_%s", order.getSide().name()))
                    .put("timeInForce", String.format("TIME_IN_FORCE_%s", order.getTimeInForce().name()))
                    .put("type", String.format("TYPE_%s", order.getType().name()))
                    .put("reference", reference);
            JSONObject submission = new JSONObject()
                    .put("orderSubmission", orderSubmission)
                    .put("pubKey", partyId)
                    .put("propagate", true);
            String token = getToken().orElseThrow(() -> new TradingException(ErrorCode.GET_VEGA_TOKEN_FAILED));
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", String.format("Bearer %s", token));
            HttpResponse<JsonNode> response = Unirest.post(String.format("%s/api/v1/command/sync", walletUrl))
                    .headers(headers)
                    .body(submission)
                    .asJson();
            if(response.getBody().toString().contains("couldn't get last block height")) {
                return submitOrder(order, partyId, attempt+1);
            }
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Cancel an order
     *
     * @param id the order ID
     * @param partyId the party ID
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> cancelOrder(
            final String id,
            final String partyId
    ) {
        return cancelOrder(id, partyId, 1);
    }

    /**
     * Submit a new order
     *
     * @param order {@link Order}
     * @param partyId the party ID
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> submitOrder(
            final Order order,
            final String partyId
    ) {
        return submitOrder(order, partyId, 1);
    }

    /**
     * Submit a new liquidity commitment
     *
     * @param liquidityProvision {@link LiquidityProvision}
     * @param partyId the party ID
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> submitLiquidityProvision(
            LiquidityProvision liquidityProvision,
            final String partyId
    ) {
        return Optional.empty();
    }

    /**
     * Amend existing liquidity commitment
     *
     * @param liquidityProvision {@link LiquidityProvision}
     * @param partyId the party ID
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> amendLiquidityProvision(
            LiquidityProvision liquidityProvision,
            final String partyId
    ) {
        return Optional.empty();
    }

    /**
     * Get an authorization token from the wallet
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> getToken() {
        try {
            JSONObject request = new JSONObject()
                    .put("wallet", walletUser)
                    .put("passphrase", walletPassword);
            HttpResponse<JsonNode> response = Unirest.post(String.format("%s/api/v1/auth/token", walletUrl))
                    .body(request)
                    .asJson();
            return Optional.of(response.getBody().getObject().getString("token"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }
}