package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.*;
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
    private VegaApiClient vegaApiClient;
    private final String PARTY_ID = "1";
    private final String MARKET_ID = "1";
    private final Double FEE = 0.001;
    private final Double SPREAD = 0.005;
    private final Integer ORDER_COUNT = 10;
    private final Double BID_SIZE_FACTOR = 1.0;
    private final Double ASK_SIZE_FACTOR = 1.0;
    private final Double BID_QUOTE_RANGE = 0.05;
    private final Double ASK_QUOTE_RANGE = 0.05;
    private final Double PRICING_SIZE_STEP = 0.1;

    @BeforeEach
    public void setup() {
        orderStore = Mockito.mock(OrderStore.class);
        marketStore = Mockito.mock(MarketStore.class);
        positionStore = Mockito.mock(PositionStore.class);
        appConfigStore = Mockito.mock(AppConfigStore.class);
        accountStore = Mockito.mock(AccountStore.class);
        vegaApiClient = Mockito.mock(VegaApiClient.class);
        liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
        dataInitializer = new DataInitializer(orderStore, marketStore, positionStore, appConfigStore, accountStore,
                liquidityCommitmentStore, vegaApiClient, PARTY_ID, MARKET_ID, FEE, SPREAD, ORDER_COUNT, BID_SIZE_FACTOR,
                ASK_SIZE_FACTOR, BID_QUOTE_RANGE, ASK_QUOTE_RANGE, PRICING_SIZE_STEP);
    }

    @Test
    public void testInitialize() {
        Mockito.when(vegaApiClient.getMarkets()).thenReturn(List.of(new Market()));
        Mockito.when(vegaApiClient.getOpenOrders(PARTY_ID)).thenReturn(List.of(new Order()));
        dataInitializer.initialize();
        Mockito.verify(marketStore, Mockito.times(1)).add(Mockito.any(Market.class));
        Mockito.verify(orderStore, Mockito.times(1)).add(Mockito.any(Order.class));
    }
}