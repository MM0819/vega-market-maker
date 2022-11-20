package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.Market;
import com.vega.protocol.store.MarketStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MarketService {

    private final MarketStore marketStore;

    public MarketService(MarketStore marketStore) {
        this.marketStore = marketStore;
    }

    /**
     * Get market by ID
     *
     * @return {@link Market}
     */
    public Market getById(
            final String marketId
    ) {
        return marketStore.getItems().stream().filter(m -> m.getId().equals(marketId)).findFirst()
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
    }
}