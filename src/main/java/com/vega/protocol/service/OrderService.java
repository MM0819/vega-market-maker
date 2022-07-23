package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    /**
     * Get the opposite {@link MarketSide}
     *
     * @param side {@link MarketSide}
     *
     * @return opposite {@link MarketSide}
     */
    public MarketSide getOtherSide(
            MarketSide side
    ) {
        return side.equals(MarketSide.BUY) ? MarketSide.SELL : MarketSide.BUY;
    }
}