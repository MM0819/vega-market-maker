package com.vega.protocol.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
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

    public VegaApiClient(@Value("${vega.wallet.url}") String walletUrl,
                         @Value("${vega.wallet.user}") String walletUser,
                         @Value("${vega.wallet.password}") String walletPassword,
                         @Value("${vega.node.url}") String nodeUrl,
                         @Value("${vega.market.id}") String marketId) {
        this.walletUrl = walletUrl;
        this.walletUser = walletUser;
        this.walletPassword = walletPassword;
        this.nodeUrl = nodeUrl;
        this.marketId = marketId;
    }

    /**
     * Get market by ID
     *
     * @param marketId the market ID
     *
     * @return {@link Optional<Market>}
     */
    public Optional<Market> getMarket(
            final String marketId
    ) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(String.format("%s/markets/%s", nodeUrl, marketId)).asJson();
            JSONObject marketObject = response.getBody().getObject().getJSONObject("market");
            JSONObject tradableInstrument = marketObject.getJSONObject("tradableInstrument");
            Market market = new Market()
                    .setName(tradableInstrument.getJSONObject("instrument").getString("name"))
                    .setSettlementAsset(tradableInstrument.getJSONObject("instrument")
                            .getJSONObject("future").getString("quoteName"))
                    .setDecimalPlaces(marketObject.getInt("decimalPlaces"))
                    .setId(marketId)
                    .setStatus(marketObject.getString("state").replace("STATE_", ""));
            return Optional.of(market);
        } catch(UnirestException | JSONException e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
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
            Map<String, Market> marketsById = new HashMap<>();
            for(int i=0; i<ordersArray.length(); i++) {
                JSONObject orderObject = ordersArray.getJSONObject(i);
                String marketId = orderObject.getString("marketId");
                Market market = marketsById.get(marketId);
                if(market == null) {
                    market = getMarket(marketId).orElse(null);
                    marketsById.put(marketId, market);
                }
                Order order = new Order()
                        .setType(OrderType.valueOf(orderObject.getString("type")
                                .replace("TYPE_", "")))
                        .setSize(BigDecimal.valueOf(orderObject.getDouble("size")))
                        .setStatus(OrderStatus.valueOf(orderObject.getString("status")
                                .replace("STATUS_", "")))
                        .setSize(BigDecimal.valueOf(orderObject.getDouble("size")))
                        .setPartyId(partyId)
                        .setMarket(market)
                        .setRemainingSize(BigDecimal.valueOf(orderObject.getDouble("remaining")))
                        .setSide(MarketSide.valueOf(orderObject.getString("side")
                                .replace("SIDE_", "")))
                        .setId(orderObject.getString("id"));
                if(order.getStatus().equals(OrderStatus.ACTIVE)) {
                    orders.add(order);
                }
            }
            return orders;
        } catch(UnirestException | JSONException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
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
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (UnirestException | JSONException e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
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
        try {
            String reference = String.format("%s-%s", partyId, UUID.randomUUID());
            JSONObject orderSubmission = new JSONObject()
                    .put("marketId", marketId)
                    .put("price", order.getPrice().toString())
                    .put("size", order.getSize().toString())
                    .put("side", String.format("SIDE_%s", order.getSide().name()))
                    .put("timeInForce", "TIME_IN_FORCE_GTC")
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
            String txHash = response.getBody().getObject().getString("txHash");
            return Optional.of(txHash);
        } catch (UnirestException | JSONException e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
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
    private Optional<String> getToken() {
        try {
            JSONObject request = new JSONObject()
                    .put("wallet", walletUser)
                    .put("passphrase", walletPassword);
            HttpResponse<JsonNode> response = Unirest.post(String.format("%s/api/v1/auth/token", walletUrl))
                    .body(request)
                    .asJson();
            return Optional.of(response.getBody().getObject().getString("token"));
        } catch (UnirestException | JSONException e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }
}