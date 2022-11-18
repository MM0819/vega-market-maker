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
    private final MarketConfigRepository marketConfigRepository = Mockito.mock(MarketConfigRepository.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);
    private final Double fee = 0.001;
    private final Double askQuoteRange = 0.02;
    private final Double bidQuoteRange = 0.02;
    private final Double askSizeFactor = 1.0;
    private final Double bidSizeFactor = 1.0;
    private final Double bboOffset = 0.0;
    private final Double commitmentBalanceRatio = 0.2;
    private final Double commitmentSpread = 0.002;
    private final Integer commitmentOrderCount = 1;
    private final Integer quoteOrderCount = 20;
    private final Double stakeBuffer = 0.2;
    private final Double minSpread = 0.0005;
    private final Double maxSpread = 0.01;

    @BeforeEach
    public void setup() {
        Mockito.when(marketStore.getItems()).thenReturn(List.of());
        marketService = new MarketService(marketStore, marketConfigRepository, tradingConfigRepository, fee,
                askQuoteRange, bidQuoteRange, askSizeFactor, bidSizeFactor, bboOffset, commitmentBalanceRatio,
                commitmentSpread, commitmentOrderCount, quoteOrderCount, stakeBuffer, minSpread, maxSpread);
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