package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.util.List;

public class MarketServiceTest {

    private MarketService marketService;
    private final VegaStore store = Mockito.mock(VegaStore.class);

    @BeforeEach
    public void setup() {
        Mockito.when(store.getMarkets()).thenReturn(List.of());
        marketService = new MarketService(store);
    }

    @Test
    public void testGetById() {
        Mockito.when(store.getMarkets()).thenReturn(List.of(
                TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                        Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT"),
                TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                        Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDC")
        ));
        Markets.Market market = marketService.getById("1");
        Assertions.assertEquals(market.getId(), "1");
    }

    @Test
    public void testGetByIdNotFound() {
        try {
            marketService.getById("3");
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.MARKET_NOT_FOUND);
        }
    }
}