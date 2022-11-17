package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.Market;
import com.vega.protocol.repository.GlobalConfigRepository;
import com.vega.protocol.repository.MarketConfigRepository;
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
    private AccountStore accountStore;
    private LiquidityCommitmentStore liquidityCommitmentStore;
    private AssetStore assetStore;
    private NetworkParameterStore networkParameterStore;
    private VegaApiClient vegaApiClient;
    private GlobalConfigRepository globalConfigRepository;
    private MarketConfigRepository marketConfigRepository;

    private final String binanceApiKey = "xxx";
    private final String binanceApiSecret = "xxx";
    private final String binanceWebSocketUrl = "xxx";
    private final Boolean binanceWebSocketEnabled = true;
    private final String igApiKey = "xxx";
    private final String igUsername = "xxx";
    private final String igPassword = "xxx";
    private final String polygonApiKey = "xxx";
    private final String polygonWebSocketUrl = "xxx";
    private final Boolean polygonWebSocketEnabled = true;
    private final String vegaApiUrl = "xxx";
    private final String vegaWalletUrl = "xxx";
    private final String vegaWebSocketUrl = "xxx";
    private final String vegaWalletUser = "xxx";
    private final String vegaWalletPassword = "xxx";
    private final Boolean vegaWebSocketEnabled = true;
    private final String naiveFlowPartyId = "xxx";


    @BeforeEach
    public void setup() {
        orderStore = Mockito.mock(OrderStore.class);
        marketStore = Mockito.mock(MarketStore.class);
        positionStore = Mockito.mock(PositionStore.class);
        accountStore = Mockito.mock(AccountStore.class);
        vegaApiClient = Mockito.mock(VegaApiClient.class);
        liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
        assetStore = Mockito.mock(AssetStore.class);
        networkParameterStore = Mockito.mock(NetworkParameterStore.class);
        globalConfigRepository = Mockito.mock(GlobalConfigRepository.class);
        marketConfigRepository = Mockito.mock(MarketConfigRepository.class);
        dataInitializer = new DataInitializer(orderStore, marketStore, positionStore, globalConfigRepository,
                marketConfigRepository, accountStore, liquidityCommitmentStore, assetStore, networkParameterStore,
                vegaApiClient, binanceApiKey, binanceApiSecret, binanceWebSocketUrl, binanceWebSocketEnabled, igApiKey,
                igUsername, igPassword, polygonApiKey, polygonWebSocketUrl, polygonWebSocketEnabled, vegaApiUrl,
                vegaWalletUrl, vegaWebSocketUrl, vegaWalletUser, vegaWalletPassword, vegaWebSocketEnabled,
                naiveFlowPartyId);
    }

    @Test
    public void testInitialize() {
        Mockito.when(vegaApiClient.getMarkets()).thenReturn(List.of(new Market()));
        Assertions.assertFalse(dataInitializer.isInitialized());
        dataInitializer.initialize();
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Mockito.verify(marketStore, Mockito.times(1)).update(Mockito.any(Market.class));
        Assertions.assertTrue(dataInitializer.isInitialized());
    }
}