package com.vega.protocol.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.MarketStore;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VegaApiClientTest {

    private static final String WALLET_URL = "http://localhost:1789";
    private static final String WALLET_USER = "trading";
    private static final String WALLET_PASSWORD = "password123";
    private static final String NODE_URL = "https://lb.testnet.vega.xyz/datanode/rest";
    private static final String MARKET_ID = "10c4b1114d2f6fda239b73d018bca55888b6018f0ac70029972a17fea0a6a56e";
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);

    private final VegaApiClient vegaApiClient = new VegaApiClient(
            WALLET_URL, WALLET_USER, WALLET_PASSWORD, NODE_URL, MARKET_ID, marketStore
    );

    // TODO - we should be mocking Unirest so that these tests are fast (and so we can also test the recursion)

    private Order newOrder() {
        return new Order()
                .setSide(MarketSide.BUY)
                .setSize(BigDecimal.ONE)
                .setPrice(BigDecimal.ONE)
                .setType(OrderType.LIMIT);
    }

    private JSONObject tokenJson() throws JSONException {
        return new JSONObject().put("token", "12345");
    }

    private JSONObject txHashJson() throws JSONException {
        return new JSONObject().put("txHash", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    }

    private JSONObject errorJson() throws JSONException {
        return new JSONObject().put("error", "couldn't get last block height");
    }

    private void mockGetToken(
            final MockedStatic<Unirest> mockStatic,
            final JSONObject jsonResponse
    ) throws Exception {
        HttpRequestWithBody request = Mockito.mock(HttpRequestWithBody.class);
        RequestBodyEntity entity = Mockito.mock(RequestBodyEntity.class);
        HttpResponse<JsonNode> response = Mockito.mock(HttpResponse.class);
        Mockito.when(request.body(Mockito.any(JSONObject.class))).thenReturn(entity);
        Mockito.when(entity.asJson()).thenReturn(response);
        Mockito.when(response.getBody()).thenReturn(new JsonNode(jsonResponse.toString()));
        mockStatic.when(() -> Unirest.post(String.format("%s/api/v1/auth/token", WALLET_URL))).thenReturn(request);
    }

    private void mockSubmitTransaction(
            final MockedStatic<Unirest> mockStatic,
            final JSONObject jsonResponse
    ) throws Exception {
        HttpRequestWithBody request = Mockito.mock(HttpRequestWithBody.class);
        RequestBodyEntity entity = Mockito.mock(RequestBodyEntity.class);
        HttpResponse<JsonNode> response = Mockito.mock(HttpResponse.class);
        Mockito.when(request.body(Mockito.any(JSONObject.class))).thenReturn(entity);
        Mockito.when(request.headers(Mockito.any())).thenReturn(request);
        Mockito.when(entity.asJson()).thenReturn(response);
        Mockito.when(response.getBody()).thenReturn(new JsonNode(jsonResponse.toString()));
        mockStatic.when(() -> Unirest.post(String.format("%s/api/v1/command/sync", WALLET_URL))).thenReturn(request);
    }

    private void mockGetRequest(
            final String path,
            final MockedStatic<Unirest> mockStatic,
            final JSONObject jsonResponse
    ) throws Exception {
        GetRequest request = Mockito.mock(GetRequest.class);
        HttpResponse<JsonNode> response = Mockito.mock(HttpResponse.class);
        Mockito.when(request.asJson()).thenReturn(response);
        Mockito.when(response.getBody()).thenReturn(new JsonNode(jsonResponse.toString()));
        mockStatic.when(() -> Unirest.get(String.format("%s%s", NODE_URL, path))).thenReturn(request);
    }

    private Optional<String> submitOrder(
            final JSONObject jsonResponse
    ) {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            mockSubmitTransaction(mockStatic, jsonResponse);
            Order order = newOrder();
            return vegaApiClient.submitOrder(order, PARTY_ID);
        } catch(Exception e) {
            Assertions.fail();
        }
        return Optional.empty();
    }

    private Optional<String> cancelOrder(
            final JSONObject jsonResponse
    ) {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            mockSubmitTransaction(mockStatic, jsonResponse);
            return vegaApiClient.cancelOrder("1", PARTY_ID);
        } catch(Exception e) {
            Assertions.fail();
        }
        return Optional.empty();
    }

    @Test
    public void testSubmitOrder() throws JSONException {
        Optional<String> txHash = submitOrder(txHashJson());
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testSubmitOrderWithWalletError() throws JSONException {
        Optional<String> txHash = submitOrder(errorJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testCancelOrder() throws JSONException {
        Optional<String> txHash = cancelOrder(txHashJson());
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testCancelOrderWithWalletError() throws JSONException {
        Optional<String> txHash = cancelOrder(errorJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testSubmitLiquidityProvision() {
        vegaApiClient.submitLiquidityProvision(new LiquidityProvision(), PARTY_ID);
    }

    @Test
    public void testAmendLiquidityProvision() {
        vegaApiClient.amendLiquidityProvision(new LiquidityProvision(), PARTY_ID);
    }

    @Test
    public void testGetMarkets() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-markets-rest.json")) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest("/markets", mockStatic, new JSONObject(marketsJson));
                List<Market> markets = vegaApiClient.getMarkets();
                Assertions.assertEquals(1, markets.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testGetOpenOrders() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-rest.json")) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest(String.format("/parties/%s/orders", PARTY_ID), mockStatic, new JSONObject(marketsJson));
                List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
                Assertions.assertEquals(1, orders.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testGetTokenWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            Optional<String> token = vegaApiClient.getToken();
            Assertions.assertTrue(token.isEmpty());
        }
    }

    @Test
    public void testCancelOrderWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            Optional<String> txHash = vegaApiClient.cancelOrder("1", PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testSubmitOrderWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            Optional<String> txHash = vegaApiClient.submitOrder(new Order(), PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testCancelOrderWithMissingToken() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, new JSONObject());
            Optional<String> txHash = vegaApiClient.cancelOrder("1", PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testSubmitOrderWithMissingToken() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, new JSONObject());
            Optional<String> txHash = vegaApiClient.submitOrder(newOrder(), PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testGetOrdersWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
            Assertions.assertEquals(0, orders.size());
        }
    }

    @Test
    public void testGetMarketsWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Market> markets = vegaApiClient.getMarkets();
            Assertions.assertEquals(0, markets.size());
        }
    }
}
