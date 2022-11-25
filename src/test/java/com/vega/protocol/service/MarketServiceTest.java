package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.util.List;
import java.util.Optional;

public class MarketServiceTest {

    private MarketService marketService;
    private VegaStore vegaStore;
    private DecimalUtils decimalUtils;
    private ReferencePriceStore referencePriceStore;

    @BeforeEach
    public void setup() {
        vegaStore = Mockito.mock(VegaStore.class);
        decimalUtils = Mockito.mock(DecimalUtils.class);
        referencePriceStore = Mockito.mock(ReferencePriceStore.class);
        Mockito.when(vegaStore.getMarkets()).thenReturn(List.of());
        marketService = new MarketService(vegaStore, decimalUtils, referencePriceStore);
    }

    @Test
    public void testGetById() {
        Mockito.when(vegaStore.getMarketById(TestingHelper.ID)).thenReturn(Optional.of(
                TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                        Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT")
        ));
        Markets.Market market = marketService.getById(TestingHelper.ID);
        Assertions.assertEquals(market.getId(), TestingHelper.ID);
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