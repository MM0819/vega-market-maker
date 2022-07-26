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
    private NetworkParameterStore networkParameterStore;
    private VegaApiClient vegaApiClient;
    private final String PARTY_ID = "1";
    private final Double FEE = 0.001;
    private final Double MIN_SPREAD = 0.003;
    private final Double MAX_SPREAD = 0.01;
    private final Integer ORDER_COUNT = 10;
    private final Double BID_SIZE_FACTOR = 1.0;
    private final Double ASK_SIZE_FACTOR = 1.0;
    private final Double BID_QUOTE_RANGE = 0.05;
    private final Double ASK_QUOTE_RANGE = 0.05;
    private final Double COMMITMENT_FACTOR = 1.0;
    private final Double COMMITMENT_SPREAD = 0.02;
    private final Integer COMMITMENT_ORDER_COUNT = 3;
    private final Double STAKE_BUFFER = 0.2;
    private final Double BBO_OFFSET = 0.0;

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
        networkParameterStore = Mockito.mock(NetworkParameterStore.class);
        dataInitializer = new DataInitializer(orderStore, marketStore, positionStore, appConfigStore, accountStore,
                liquidityCommitmentStore, assetStore, networkParameterStore, vegaApiClient, PARTY_ID, FEE, MIN_SPREAD,
                MAX_SPREAD, COMMITMENT_SPREAD, ORDER_COUNT, BID_SIZE_FACTOR, ASK_SIZE_FACTOR, COMMITMENT_FACTOR,
                BID_QUOTE_RANGE, ASK_QUOTE_RANGE, COMMITMENT_ORDER_COUNT, STAKE_BUFFER, BBO_OFFSET);
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