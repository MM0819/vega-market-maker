package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.store.VegaStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vega.Markets;
import vega.Vega;

@Slf4j
@Service
public class MarketService {

    private final VegaStore vegaStore;

    public MarketService(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    /**
     * Get market by ID
     *
     * @return {@link Markets.Market}
     */
    public Markets.Market getById(
            final String marketId
    ) {
        return vegaStore.getMarketById(marketId)
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
    }

    public Vega.MarketData getDataById(
            final String marketId
    ) {
        return vegaStore.getMarketDataById(marketId)
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_DATA_NOT_FOUND));
    }
}