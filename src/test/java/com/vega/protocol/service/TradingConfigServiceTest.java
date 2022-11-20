package com.vega.protocol.service;

import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.repository.TradingConfigRepository;
import org.mockito.Mockito;

public class TradingConfigServiceTest {
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
}
