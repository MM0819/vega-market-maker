package com.vega.protocol.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import com.vega.protocol.constant.LiquidityCommitmentStatus;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.constant.TimeInForce;
import com.vega.protocol.model.*;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.AssetStore;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.utils.DecimalUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VegaApiClientTest {

    private static final String WALLET_URL = "http://localhost:1789";
    private static final String WALLET_USER = "trading";
    private static final String WALLET_PASSWORD = "password123";
    private static final String NODE_URL = "https://api.n11.testnet.vega.xyz";
    private static final String MARKET_ID = "10c4b1114d2f6fda239b73d018bca55888b6018f0ac70029972a17fea0a6a56e";
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);
    private final AssetStore assetStore = Mockito.mock(AssetStore.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);
    private final OrderService orderService = Mockito.mock(OrderService.class);

    private final VegaApiClient vegaApiClient = new VegaApiClient(
            WALLET_URL, WALLET_USER, WALLET_PASSWORD, NODE_URL, MARKET_ID,
            marketStore, assetStore, decimalUtils, orderService
    );

    private Order newOrder() {
        Market market = new Market()
                .setId("12345")
                .setSettlementAsset("USDT")
                .setDecimalPlaces(5)
                .setPositionDecimalPlaces(3);
        return new Order()
                .setMarket(market)
                .setTimeInForce(TimeInForce.GTC)
                .setSide(MarketSide.BUY)
                .setSize(BigDecimal.ONE)
                .setPrice(BigDecimal.ONE)
                .setType(OrderType.LIMIT);
    }

    private LiquidityCommitment newLiquidityCommitment() {
        Market market = new Market()
                .setSettlementAsset("USDT")
                .setId("12345")
                .setDecimalPlaces(5)
                .setPositionDecimalPlaces(3);
        return new LiquidityCommitment()
                .setMarket(market)
                .setFee(BigDecimal.valueOf(0.001))
                .setStatus(LiquidityCommitmentStatus.ACTIVE)
                .setCommitmentAmount(BigDecimal.ONE)
                .setAsks(new ArrayList<>())
                .setBids(new ArrayList<>());
    }

    private JSONObject tokenJson() throws JSONException {
        return new JSONObject().put("token", "12345");
    }

    private JSONObject txHashJson() throws JSONException {
        return new JSONObject().put("txHash", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    }

    private JSONObject missingBlockJson() throws JSONException {
        return new JSONObject().put("error", "couldn't get last block height");
    }

    private JSONObject errorGenericJson() throws JSONException {
        return new JSONObject().put("error", "something went wrong");
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
            final JSONObject jsonResponse,
            final int statusCode
    ) throws Exception {
        GetRequest request = Mockito.mock(GetRequest.class);
        HttpResponse<JsonNode> response = Mockito.mock(HttpResponse.class);
        Mockito.when(request.asJson()).thenReturn(response);
        Mockito.when(response.getBody()).thenReturn(new JsonNode(jsonResponse.toString()));
        Mockito.when(response.getStatus()).thenReturn(statusCode);
        mockStatic.when(() -> Unirest.get(String.format("%s%s", NODE_URL, path))).thenReturn(request);
    }

    private void getAccounts(
            final Optional<Asset> asset,
            final int expectedAccounts,
            final int statusCode
    ) {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        if(asset.isPresent()) {
            Mockito.when(assetStore.getItems()).thenReturn(Collections.singletonList(new Asset().setSymbol("USDT")
                    .setDecimalPlaces(1).setId("72f051ea66686ae004086f1ad086866f720f25896319abf3427cae101a58d985")));
        }
        Mockito.when(assetStore.getById(Mockito.anyString())).thenReturn(asset);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-accounts-rest.json")) {
                String accountsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest(String.format("/accounts?filter.partyIds=%s", PARTY_ID), mockStatic, new JSONObject(accountsJson), statusCode);
                List<Account> accounts = vegaApiClient.getAccounts(PARTY_ID);
                Assertions.assertEquals(expectedAccounts, accounts.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private void getPositions(
            final Optional<Market> market,
            final int expectedPositions,
            final int statusCode
    ) {
        Mockito.when(marketStore.getById(Mockito.any())).thenReturn(market);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-positions-rest.json")) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest(String.format("/positions?partyId=%s", PARTY_ID), mockStatic, new JSONObject(marketsJson), statusCode);
                List<Position> positions = vegaApiClient.getPositions(PARTY_ID);
                Assertions.assertEquals(expectedPositions, positions.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private void getOpenOrders(
            final Optional<Market> market,
            final int expectedOrders,
            final int statusCode
    ) {
        Mockito.when(marketStore.getById(Mockito.any())).thenReturn(market);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-rest.json")) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest(String.format("/orders?partyId=%s&liveOnly=true", PARTY_ID), mockStatic, new JSONObject(marketsJson), statusCode);
                List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
                Assertions.assertEquals(expectedOrders, orders.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private void getMarkets(
            final Optional<Asset> asset,
            final int count,
            final int statusCode
    ) {
        Mockito.when(assetStore.getById(Mockito.any())).thenReturn(asset);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-markets-rest.json")) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest("/markets", mockStatic, new JSONObject(marketsJson), statusCode);
                List<Market> markets = vegaApiClient.getMarkets();
                Assertions.assertEquals(count, markets.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private void getLiquidityCommitment(
            final Optional<Market> market,
            final Asset asset,
            final boolean isPresent,
            final int idx,
            final int statusCode
    ) {
        Mockito.when(marketStore.getById(Mockito.any())).thenReturn(market);
        Mockito.when(assetStore.getItems()).thenReturn(Collections.singletonList(asset));
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader()
                    .getResourceAsStream(String.format("vega-liquidity-provisions-rest-%s.json", idx))) {
                String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest(String.format("/liquidity/provisions?partyId=%s", PARTY_ID),
                        mockStatic, new JSONObject(marketsJson), statusCode);
                List<LiquidityCommitment> commitments = vegaApiClient.getLiquidityCommitments(PARTY_ID);
                Assertions.assertEquals(commitments.size(), isPresent ? 1 : 0);
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private void getAssets(
            final int count,
            final int statusCode
    ) {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-assets-rest.json")) {
                String accountsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
                mockGetToken(mockStatic, tokenJson());
                mockGetRequest("/assets", mockStatic, new JSONObject(accountsJson), statusCode);
                List<Asset> assets = vegaApiClient.getAssets();
                Assertions.assertEquals(count, assets.size());
            } catch (Exception e) {
                Assertions.fail();
            }
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    private Optional<String> submitOrder(
            final JSONObject jsonResponse
    ) {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
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

    private Optional<String> amendOrder(
            final JSONObject jsonResponse
    ) {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            mockSubmitTransaction(mockStatic, jsonResponse);
            Market market = new Market().setDecimalPlaces(1).setPositionDecimalPlaces(1);
            return vegaApiClient.amendOrder("1", BigDecimal.ONE, BigDecimal.ONE, market, PARTY_ID);
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

    private Optional<String> submitLiquidityCommitment(
            final JSONObject jsonResponse,
            final boolean amendment
    ) throws JSONException {
        Mockito.when(assetStore.getItems()).thenReturn(Collections.singletonList(
                new Asset().setSymbol("USDT").setDecimalPlaces(1)));
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        Mockito.when(orderService.buildLiquidityOrders(Mockito.anyInt(), Mockito.anyList())).thenReturn(new JSONArray());
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            mockSubmitTransaction(mockStatic, jsonResponse);
            LiquidityCommitment liquidityCommitment = newLiquidityCommitment();
            return vegaApiClient.submitLiquidityCommitment(liquidityCommitment, PARTY_ID, amendment);
        } catch(Exception e) {
            Assertions.fail();
        }
        return Optional.empty();
    }

    private Asset getAsset() {
        return new Asset().setSymbol("USDT").setDecimalPlaces(1)
                .setId("72f051ea66686ae004086f1ad086866f720f25896319abf3427cae101a58d985");
    }

    @Test
    public void testSubmitLiquidityCommitment() throws JSONException {
        Optional<String> txHash = submitLiquidityCommitment(txHashJson(), false);
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testSubmitLiquidityAmendment() throws JSONException {
        Optional<String> txHash = submitLiquidityCommitment(txHashJson(), true);
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testSubmitLiquidityCommitmentWithWalletError() throws JSONException {
        Optional<String> txHash = submitLiquidityCommitment(missingBlockJson(), false);
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testSubmitLiquidityCommitmentWithGenericError() throws JSONException {
        Optional<String> txHash = submitLiquidityCommitment(errorGenericJson(), false);
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testSubmitOrder() throws JSONException {
        Optional<String> txHash = submitOrder(txHashJson());
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testSubmitOrderWithWalletError() throws JSONException {
        Optional<String> txHash = submitOrder(missingBlockJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testSubmitOrderWithGenericError() throws JSONException {
        Optional<String> txHash = submitOrder(errorGenericJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testAmendOrder() throws JSONException {
        Optional<String> txHash = amendOrder(txHashJson());
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testAmendOrderWithWalletError() throws JSONException {
        Optional<String> txHash = amendOrder(missingBlockJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testAmendOrderWithGenericError() throws JSONException {
        Optional<String> txHash = amendOrder(errorGenericJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testCancelOrder() throws JSONException {
        Optional<String> txHash = cancelOrder(txHashJson());
        Assertions.assertTrue(txHash.isPresent());
    }

    @Test
    public void testCancelOrderWithWalletError() throws JSONException {
        Optional<String> txHash = cancelOrder(missingBlockJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testCancelOrderWithGenericError() throws JSONException {
        Optional<String> txHash = cancelOrder(errorGenericJson());
        Assertions.assertTrue(txHash.isEmpty());
    }

    @Test
    public void testGetAssets() {
        getAssets(20, 200);
    }

    @Test
    public void testGetAssetsWithApiError() {
        getAssets(0, 500);
    }

    @Test
    public void testGetAccounts() {
        getAccounts(Optional.of(new Asset().setDecimalPlaces(1)), 2, 200);
    }

    @Test
    public void testGetAccountsWhenAssetNotfound() {
        getAccounts(Optional.empty(), 0, 200);
    }

    @Test
    public void testGetAccountsWithApiError() {
        getAccounts(Optional.empty(), 0, 500);
    }

    @Test
    public void testGetMarkets() {
        getMarkets(Optional.of(new Asset()), 14, 200);
    }

    @Test
    public void testGetMarketsMissingAsset() {
        getMarkets(Optional.empty(), 0, 200);
    }

    @Test
    public void testGetMarketsWithApiError() {
        getMarkets(Optional.empty(), 0, 500);
    }

    @Test
    public void testGetPositions() {
        getPositions(Optional.of(new Market()), 3, 200);
    }

    @Test
    public void testGetPositionsMissingMarket() {
        getPositions(Optional.empty(), 0, 200);
    }

    @Test
    public void testGetPositionsWithApiError() {
        getPositions(Optional.empty(), 0, 500);
    }

    @Test
    public void testGetLiquidityCommitment() {
        getLiquidityCommitment(Optional.of(new Market().setSettlementAsset("USDT")), getAsset(), true, 1, 200);
    }

    @Test
    public void testGetLiquidityCommitmentMissingMarket() {
        getLiquidityCommitment(Optional.empty(), getAsset(), false, 1, 200);
    }

    @Test
    public void testGetLiquidityCommitmentMissingAsset() {
        getLiquidityCommitment(Optional.of(new Market().setSettlementAsset("USDT")),
                getAsset().setSymbol("BTC"), false, 1, 200);
    }

    @Test
    public void testGetLiquidityCommitmentEmptyResponse() {
        getLiquidityCommitment(Optional.empty(), getAsset(), false, 0, 200);
    }

    @Test
    public void testGetLiquidityCommitmentWithApiError() {
        getLiquidityCommitment(Optional.empty(), getAsset(), false, 0, 500);
    }

    @Test
    public void testGetOpenOrders() {
        getOpenOrders(Optional.of(new Market()), 1, 200);
    }

    @Test
    public void testGetOpenOrdersMissingMarket() {
        getOpenOrders(Optional.empty(), 0, 200);
    }

    @Test
    public void testGetOpenOrdersWithApiError() {
        getOpenOrders(Optional.empty(), 0, 500);
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
    public void testSubmitLiquidityCommitmentWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, tokenJson());
            Optional<String> txHash = vegaApiClient.submitLiquidityCommitment(
                    new LiquidityCommitment(), PARTY_ID, false);
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
    public void testAmendOrderWithMissingToken() {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, new JSONObject());
            Market market = new Market().setDecimalPlaces(1).setPositionDecimalPlaces(1);
            Optional<String> txHash = vegaApiClient.amendOrder("1",
                    BigDecimal.ONE, BigDecimal.ONE, market, PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testSubmitOrderWithMissingToken() {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, new JSONObject());
            Optional<String> txHash = vegaApiClient.submitOrder(newOrder(), PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        } catch(Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testSubmitLiquidityCommitmentWithMissingToken() {
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyInt(), Mockito.any(BigDecimal.class)))
                .thenReturn(BigDecimal.ONE);
        Mockito.when(assetStore.getItems()).thenReturn(List.of(new Asset().setSymbol("USDT").setDecimalPlaces(1)));
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            mockGetToken(mockStatic, new JSONObject());
            Optional<String> txHash = vegaApiClient.submitLiquidityCommitment(
                    newLiquidityCommitment(), PARTY_ID, false);
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

    @Test
    public void testGetAssetsWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Asset> assets = vegaApiClient.getAssets();
            Assertions.assertEquals(0, assets.size());
        }
    }

    @Test
    public void testGetPositionsWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Position> positions = vegaApiClient.getPositions(PARTY_ID);
            Assertions.assertEquals(0, positions.size());
        }
    }
}
