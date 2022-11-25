package com.vega.protocol.initializer;

import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.repository.GlobalConfigRepository;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.store.VegaStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.util.List;

public class DataInitializerTest {

    private DataInitializer dataInitializer;
    private VegaStore vegaStore;
    private VegaGrpcClient vegaGrpcClient;
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
        vegaStore = Mockito.mock(VegaStore.class);
        vegaGrpcClient = Mockito.mock(VegaGrpcClient.class);
        globalConfigRepository = Mockito.mock(GlobalConfigRepository.class);
        marketConfigRepository = Mockito.mock(MarketConfigRepository.class);
        dataInitializer = new DataInitializer(vegaStore, globalConfigRepository, marketConfigRepository, vegaGrpcClient,
                binanceApiKey, binanceApiSecret, binanceWebSocketUrl, binanceWebSocketEnabled, igApiKey, igUsername,
                igPassword, polygonApiKey, polygonWebSocketUrl, polygonWebSocketEnabled, vegaApiUrl, vegaWalletUrl,
                vegaWebSocketUrl, vegaWalletUser, vegaWalletPassword, vegaWebSocketEnabled, naiveFlowPartyId);
    }

    @Test
    public void testInitialize() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT");
        Mockito.when(vegaGrpcClient.getMarkets()).thenReturn(List.of(market));
        Assertions.assertFalse(dataInitializer.isInitialized());
        dataInitializer.initialize();
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Mockito.verify(vegaStore, Mockito.times(1))
                .updateMarket(Mockito.any(Markets.Market.class));
        Assertions.assertTrue(dataInitializer.isInitialized());
    }
}