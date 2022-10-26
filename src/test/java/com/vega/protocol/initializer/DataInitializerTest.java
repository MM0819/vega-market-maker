package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class DataInitializerTest {

    private DataInitializer dataInitializer;
    private OrderStore orderStore;
    private MarketStore marketStore;
    private PositionStore positionStore;
    private AppConfigStore appConfigStore;
    private AccountStore accountStore;
    private LiquidityCommitmentStore liquidityCommitmentStore;
    private AssetStore assetStore;
    private VegaApiClient vegaApiClient;
    private final String PARTY_ID = "1";
    private final Double FEE = 0.001;
    private final Double SPREAD = 0.005;
    private final Integer ORDER_COUNT = 10;
    private final Double BID_SIZE_FACTOR = 1.0;
    private final Double ASK_SIZE_FACTOR = 1.0;
    private final Double BID_QUOTE_RANGE = 0.05;
    private final Double ASK_QUOTE_RANGE = 0.05;
    private final Double PRICING_SIZE_STEP = 0.1;
    private final Double COMMITMENT_FACTOR = 1.0;
    private final Double COMMITMENT_SPREAD = 0.02;
    private final Integer COMMITMENT_ORDER_COUNT = 3;

    @BeforeEach
    public void setup() {
        orderStore = Mockito.mock(OrderStore.class);
        marketStore = Mockito.mock(MarketStore.class);
        positionStore = Mockito.mock(PositionStore.class);
        appConfigStore = Mockito.mock(AppConfigStore.class);
        accountStore = Mockito.mock(AccountStore.class);
        vegaApiClient = Mockito.mock(VegaApiClient.class);
        liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
        assetStore = Mockito.mock(AssetStore.class);
        dataInitializer = new DataInitializer(orderStore, marketStore, positionStore, appConfigStore, accountStore,
                liquidityCommitmentStore, assetStore, vegaApiClient, PARTY_ID, FEE, SPREAD, COMMITMENT_SPREAD
                , ORDER_COUNT, BID_SIZE_FACTOR, ASK_SIZE_FACTOR, COMMITMENT_FACTOR, BID_QUOTE_RANGE, ASK_QUOTE_RANGE,
                PRICING_SIZE_STEP, COMMITMENT_ORDER_COUNT);
    }

    @Test
    public void testInitialize() {
        Mockito.when(vegaApiClient.getMarkets()).thenReturn(List.of(new Market()));
        Mockito.when(vegaApiClient.getOpenOrders(PARTY_ID)).thenReturn(List.of(new Order()));
        Assertions.assertFalse(dataInitializer.isInitialized());
        dataInitializer.initialize();
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Mockito.verify(marketStore, Mockito.times(1)).update(Mockito.any(Market.class));
        Mockito.verify(orderStore, Mockito.times(1)).update(Mockito.any(Order.class));
        Assertions.assertTrue(dataInitializer.isInitialized());
    }
}