package com.vega.protocol.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.vega.protocol.constant.*;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.*;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.AssetStore;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private final AssetStore assetStore;
    private final DecimalUtils decimalUtils;
    private final OrderService orderService;

    public VegaApiClient(@Value("${vega.wallet.url}") String walletUrl,
                         @Value("${vega.wallet.user}") String walletUser,
                         @Value("${vega.wallet.password}") String walletPassword,
                         @Value("${vega.node.url}") String nodeUrl,
                         @Value("${vega.market.id}") String marketId,
                         MarketStore marketStore,
                         AssetStore assetStore,
                         DecimalUtils decimalUtils,
                         OrderService orderService) {
        this.walletUrl = walletUrl;
        this.walletUser = walletUser;
        this.walletPassword = walletPassword;
        this.nodeUrl = nodeUrl;
        this.marketId = marketId;
        this.marketStore = marketStore;
        this.assetStore = assetStore;
        this.decimalUtils = decimalUtils;
        this.orderService = orderService;
    }

    /**
     * Get LP commitment for given party and market
     *
     * @param partyId the party ID
     *
     * @return {@link Optional<LiquidityCommitment>}
     */
    public List<LiquidityCommitment> getLiquidityCommitments(
            final String partyId
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(
                    String.format("%s/liquidity/provisions?partyId=%s", nodeUrl, partyId))
                    .asJson();
            JSONArray liquidityProvisionsArray = response.getBody().getObject()
                    .getJSONObject("liquidityProvisions").getJSONArray("edges");
            List<LiquidityCommitment> commitments = new ArrayList<>();
            for(int i=0; i<liquidityProvisionsArray.length(); i++) {
                JSONObject liquidityProvisionObject = liquidityProvisionsArray.getJSONObject(i).getJSONObject("node");
                Market market = marketStore.getById(marketId)
                        .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
                BigDecimal commitmentAmount = BigDecimal.valueOf(
                        liquidityProvisionObject.getDouble("commitmentAmount"));
                LiquidityCommitmentStatus status = LiquidityCommitmentStatus.valueOf(
                        liquidityProvisionObject.getString("status").replace("STATUS_", ""));
                String id = liquidityProvisionObject.getString("id");
                BigDecimal fee = BigDecimal.valueOf(liquidityProvisionObject.getDouble("fee"));
                JSONArray sellsArray = liquidityProvisionObject.getJSONArray("sells");
                JSONArray buysArray = liquidityProvisionObject.getJSONArray("buys");
                List<LiquidityCommitmentOffset> bids = orderService.parseLiquidityOrders(
                        buysArray, market.getDecimalPlaces(), false);
                List<LiquidityCommitmentOffset> asks = orderService.parseLiquidityOrders(
                        sellsArray, market.getDecimalPlaces(), false);
                LiquidityCommitment liquidityCommitment = new LiquidityCommitment()
                        .setCommitmentAmount(decimalUtils.convertToDecimals(
                                market.getDecimalPlaces(), commitmentAmount))
                        .setMarket(market)
                        .setStatus(status)
                        .setPartyId(partyId)
                        .setBids(bids)
                        .setAsks(asks)
                        .setId(id)
                        .setFee(fee);
                commitments.add(liquidityCommitment);
            }
            return commitments;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get assets
     *
     * @return {@link List<Asset>}
     */
    public List<Asset> getAssets() {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/assets", nodeUrl)).asJson();
            JSONArray assetsArray = response.getBody().getObject().getJSONObject("assets").getJSONArray("edges");
            List<Asset> assets = new ArrayList<>();
            for(int i=0; i<assetsArray.length(); i++) {
                JSONObject assetObject = assetsArray.getJSONObject(i).getJSONObject("node");
                String id = assetObject.getString("id");
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
                assets.add(asset);
            }
            return assets;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
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
            String url = String.format("%s/accounts?filter.partyIds=%s", nodeUrl, partyId);
            HttpResponse<JsonNode> response = Unirest.get(url).asJson();
            List<Account> accounts = new ArrayList<>();
            JSONArray accountsArray = response.getBody().getObject().getJSONObject("accounts").getJSONArray("edges");
            for(int i=0; i<accountsArray.length(); i++) {
                JSONObject accountObject = accountsArray.getJSONObject(i).getJSONObject("account");
                String assetId = accountObject.getString("asset");
                Asset asset = assetStore.getById(assetId)
                        .orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
                String marketId = accountObject.getString("marketId");
                BigDecimal balance = BigDecimal.valueOf(accountObject.getDouble("balance"));
                AccountType type = AccountType.valueOf(accountObject.getString("type")
                        .replace("ACCOUNT_TYPE_", ""));
                String id = String.format("%s-%s-%s", asset.getSymbol(), partyId, type);
                if(!type.equals(AccountType.GENERAL)) {
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
            JSONArray marketsArray = response.getBody().getObject().getJSONObject("markets").getJSONArray("edges");
            List<Market> markets = new ArrayList<>();
            for(int i=0; i<marketsArray.length(); i++) {
                JSONObject marketObject = marketsArray.getJSONObject(i).getJSONObject("node");
                JSONObject tradableInstrument = marketObject.getJSONObject("tradableInstrument");
                String id = marketObject.getString("id");
                String name = tradableInstrument.getJSONObject("instrument").getString("name");
                String settlementAssetId = tradableInstrument.getJSONObject("instrument")
                        .getJSONObject("future").getString("settlementAsset");
                Asset settlementAsset = assetStore.getById(settlementAssetId)
                        .orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
                int decimalPlaces = marketObject.getInt("decimalPlaces");
                MarketState state = MarketState.valueOf(marketObject.getString("state")
                        .replace("STATE_", ""));
                MarketTradingMode tradingMode = MarketTradingMode.valueOf(marketObject.getString("tradingMode")
                        .replace("TRADING_MODE_", ""));
                Market market = new Market()
                        .setName(name)
                        .setSettlementAsset(settlementAsset.getSymbol())
                        .setDecimalPlaces(decimalPlaces)
                        .setId(id)
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
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/positions?partyId=%s", nodeUrl, partyId))
                    .asJson();
            List<Position> positions = new ArrayList<>();
            JSONArray positionsArray = response.getBody().getObject().getJSONObject("positions").getJSONArray("edges");
            for(int i=0; i<positionsArray.length(); i++) {
                JSONObject positionObject = positionsArray.getJSONObject(i).getJSONObject("node");
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
            String url = String.format("%s/orders?partyId=%s&liveOnly=true", nodeUrl, partyId);
            HttpResponse<JsonNode> response = Unirest.get(url).asJson();
            List<Order> orders = new ArrayList<>();
            JSONArray ordersArray = response.getBody().getObject().getJSONObject("orders").getJSONArray("edges");
            for(int i=0; i<ordersArray.length(); i++) {
                JSONObject orderObject = ordersArray.getJSONObject(i).getJSONObject("node");
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
                log.info("Trying to cancel {} again...", id);
                return cancelOrder(id, partyId, attempt+1);
            }
            if(response.getBody().toString().contains("error")) {
                throw new TradingException(response.getBody().toString());
            }
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Amend order
     *
     * @param orderId the order ID
     * @param sizeDelta the change in size
     * @param price the order price
     * @param market {@link Market}
     * @param partyId the party ID
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> amendOrder(
            final String orderId,
            final BigDecimal sizeDelta,
            final BigDecimal price,
            final Market market,
            final String partyId
    ) {
        return amendOrder(orderId, sizeDelta, price, market, partyId, 1);
    }

    /**
     * Amend order with recursive retry
     *
     * @param orderId the order ID
     * @param sizeDelta the change in size
     * @param price the order price
     * @param market {@link Market}
     * @param partyId the party ID
     * @param attempt the attempt count
     *
     * @return {@link Optional<String>}
     */
    private Optional<String> amendOrder(
            final String orderId,
            final BigDecimal sizeDelta,
            final BigDecimal price,
            final Market market,
            final String partyId,
            final int attempt
    ) {
        if(attempt >= 10) {
            return Optional.empty();
        }
        try {
            JSONObject orderAmendment = new JSONObject()
                    .put("marketId", market.getId())
                    .put("sizeDelta", decimalUtils.convertFromDecimals(
                            market.getPositionDecimalPlaces(), sizeDelta).toBigInteger().toString())
                    .put("price", new JSONObject()
                            .put("value", decimalUtils.convertFromDecimals(market.getDecimalPlaces(), price)
                                    .toBigInteger().toString()))
                    .put("orderId", orderId);
            JSONObject submission = new JSONObject()
                    .put("orderAmendment", orderAmendment)
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
                log.info("Trying to amend order again...");
                return amendOrder(orderId, sizeDelta, price, market, partyId, attempt+1);
            }
            if(response.getBody().toString().contains("error")) {
                throw new TradingException(response.getBody().toString());
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
    private Optional<String> submitOrder(
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
                log.info("Trying to create new order again...");
                return submitOrder(order, partyId, attempt+1);
            }
            if(response.getBody().toString().contains("error")) {
                throw new TradingException(response.getBody().toString());
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
     * @param liquidityCommitment {@link LiquidityCommitment}
     * @param partyId the party ID
     * @param amendment true if amending an existing commitment
     *
     * @return {@link Optional<String>}
     */
    public Optional<String> submitLiquidityCommitment(
            final LiquidityCommitment liquidityCommitment,
            final String partyId,
            final boolean amendment
    ) {
        return submitLiquidityCommitment(liquidityCommitment, partyId, amendment, 1);
    }

    /**
     * Submit liquidity commitment with recursive retry
     *
     * @param liquidityCommitment the liquidity commitment
     * @param partyId the party ID
     * @param amendment true if amending an existing commitment
     * @param attempt the attempt count
     *
     * @return {@link Optional<String>}
     */
    private Optional<String> submitLiquidityCommitment(
            final LiquidityCommitment liquidityCommitment,
            final String partyId,
            final boolean amendment,
            final int attempt
    ) {
        if(attempt >= 10) {
            return Optional.empty();
        }
        try {
            Market market = liquidityCommitment.getMarket();
            BigDecimal commitmentAmount = liquidityCommitment.getCommitmentAmount();
            JSONObject liquidityProvisionSubmission = new JSONObject()
                    .put("marketId", market.getId())
                    .put("commitmentAmount", decimalUtils.convertFromDecimals(
                            market.getPositionDecimalPlaces(), commitmentAmount).toBigInteger().toString())
                    .put("fee", liquidityCommitment.getFee().toString())
                    .put("buys", orderService.buildLiquidityOrders(
                            market.getDecimalPlaces(), liquidityCommitment.getBids()))
                    .put("sells", orderService.buildLiquidityOrders(
                            market.getDecimalPlaces(), liquidityCommitment.getAsks()));
            String cmd = !amendment ? "liquidityProvisionSubmission" : "liquidityProvisionAmendment";
            JSONObject submission = new JSONObject()
                    .put(cmd, liquidityProvisionSubmission)
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
                return submitLiquidityCommitment(liquidityCommitment, partyId, amendment, attempt+1);
            }
            if(response.getBody().toString().contains("error")) {
                throw new TradingException(response.getBody().getObject().getString("error"));
            }
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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