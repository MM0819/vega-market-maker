package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.model.Market;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.store.MarketStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class MarketServiceTest {

    private MarketService marketService;
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);

    @BeforeEach
    public void setup() {
        Mockito.when(marketStore.getItems()).thenReturn(List.of());
        marketService = new MarketService(marketStore);
    }

    @Test
    public void testGetById() {
        Mockito.when(marketStore.getItems()).thenReturn(List.of(
                new Market().setId("1"),
                new Market().setId("2")
        ));
        Market market = marketService.getById("1");
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