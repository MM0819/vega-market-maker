package com.vega.protocol.ws;

import com.vega.protocol.model.*;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.*;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class VegaWebSocketClientTest {

    private VegaWebSocketClient vegaWebSocketClient;
    private MarketStore marketStore;
    private OrderStore orderStore;
    private PositionStore positionStore;
    private AccountStore accountStore;
    private AssetStore assetStore;
    private LiquidityCommitmentStore liquidityCommitmentStore;
    private DecimalUtils decimalUtils;
    private OrderService orderService;
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";
    private static final String MARKET_ID = "c6233d79a53a81b9d9d889c5beb42baaa1e3eb412d19bfd854dfa35309ce4190";

    @BeforeEach
    public void setup() {
        marketStore = Mockito.mock(MarketStore.class);
        orderStore = Mockito.mock(OrderStore.class);
        positionStore = Mockito.mock(PositionStore.class);
        accountStore = Mockito.mock(AccountStore.class);
        assetStore = Mockito.mock(AssetStore.class);
        decimalUtils = Mockito.mock(DecimalUtils.class);
        orderService = Mockito.mock(OrderService.class);
        liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
        vegaWebSocketClient = new VegaWebSocketClient(PARTY_ID, MARKET_ID, marketStore, orderStore, positionStore,
                accountStore, assetStore, liquidityCommitmentStore, decimalUtils, orderService,
                URI.create("wss://api.n11.testnet.vega.xyz/graphql"));
    }

    private void handleMarkets(
            final Optional<Asset> asset,
            final int count
    ) {
        if(count > 0) {
            Mockito.when(marketStore.getById("c6233d79a53a81b9d9d889c5beb42baaa1e3eb412d19bfd854dfa35309ce4190"))
                    .thenReturn(Optional.of(new Market().setSettlementAsset("BTC")));
            Mockito.when(marketStore.getById("7738ae422f8a905a618cb5b113e1267f1d288417361741ed033762f89f64637d"))
                    .thenReturn(Optional.of(new Market().setSettlementAsset("USDT")));
        }
        Mockito.when(assetStore.getById(Mockito.any())).thenReturn(asset);
        asset.ifPresent(value -> Mockito.when(assetStore.getItems()).thenReturn(List.of(value)));
        try(InputStream is = getClass().getClassLoader()
                .getResourceAsStream(String.format("vega-markets-ws-%s.json", count == 0 ? count + 1 : count))) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(marketStore, Mockito.times(count > 1 ? count - 1 : count)).update(Mockito.any(Market.class));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }

    private void handlePositions(int count) {
        Mockito.when(marketStore.getById(Mockito.any())).thenReturn(Optional.of(new Market()));
        try(InputStream is = getClass().getClassLoader()
                .getResourceAsStream(String.format("vega-positions-ws-%s.json", count))) {
            String positionsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(positionsJson);
            Mockito.verify(positionStore, Mockito.times(count)).update(Mockito.any(Position.class));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }

    private void handleAccounts(int count) {
        Mockito.when(assetStore.getById(Mockito.any())).thenReturn(Optional.of(new Asset().setDecimalPlaces(1)));
        try(InputStream is = getClass().getClassLoader()
                .getResourceAsStream(String.format("vega-accounts-ws-%s.json", count))) {
            String accountsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(accountsJson);
            Mockito.verify(accountStore, Mockito.times(count)).update(Mockito.any(Account.class));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }

    private void handleLiquidityCommitments(int count) {
        if(count > 0) {
            Mockito.when(marketStore.getById(Mockito.any())).thenReturn(Optional.of(new Market()));
        }
        Mockito.when(assetStore.getById(Mockito.any())).thenReturn(Optional.of(new Asset().setDecimalPlaces(1)));
        try(InputStream is = getClass().getClassLoader()
                .getResourceAsStream("vega-liquidity-provisions-ws.json")) {
            String accountsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(accountsJson);
            Mockito.verify(liquidityCommitmentStore, Mockito.times(count))
                    .update(Mockito.any(LiquidityCommitment.class));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }

    @Test
    public void testHandleMarkets() {
        handleMarkets(Optional.of(new Asset().setSymbol("USDT").setDecimalPlaces(1)), 1);
    }

    @Test
    public void testHandleMarketsMissingAsset() {
        handleMarkets(Optional.empty(), 0);
    }

    @Test
    public void testHandleMaketsMany() {
        handleMarkets(Optional.of(new Asset().setSymbol("USDT").setDecimalPlaces(1)), 2);
    }

    @Test
    public void testHandlePositions() {
        handlePositions(1);
    }

    @Test
    public void testHandlePositionsMany() {
        handlePositions(2);
    }

    @Test
    public void testHandleLiquidityCommitments() {
        handleLiquidityCommitments(1);
    }

    @Test
    public void testHandleLiquidityCommitmentsMissingMarket() {
        handleLiquidityCommitments(0);
    }

    @Test
    public void testHandleAccounts() {
        handleAccounts(1);
    }

    @Test
    public void testHandleAccountsMany() {
        handleAccounts(4);
    }

    @Test
    public void testHandleOrders() {
        Mockito.when(marketStore.getById(Mockito.any())).thenReturn(Optional.of(new Market()));
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-ws.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(orderStore, Mockito.times(3)).update(Mockito.any(Order.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleMarketsWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-markets-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(marketStore, Mockito.times(0)).update(Mockito.any(Market.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleOrdersWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(orderStore, Mockito.times(0)).update(Mockito.any(Order.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandlePositionsWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-positions-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(positionStore, Mockito.times(0)).update(Mockito.any(Position.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleAccountsWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-accounts-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(accountStore, Mockito.times(0)).update(Mockito.any(Account.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleUnsupportedMessage() throws JSONException {
        vegaWebSocketClient.onMessage(new JSONObject()
                .put("id", "error")
                .put("payload", new JSONObject().put("data", new JSONObject()))
                .toString());
    }

    @Test
    public void testOnMessageWithInvalidJSON() {
        vegaWebSocketClient.onMessage("invalid JSON");
    }

    @Test
    public void testOnError() {
        vegaWebSocketClient.onError(new RuntimeException());
    }

    @Test
    public void testOnOpenError() {
        vegaWebSocketClient.onOpen(new HandshakeImpl1Server());
    }
}