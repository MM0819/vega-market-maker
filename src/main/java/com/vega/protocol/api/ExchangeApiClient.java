package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.Position;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExchangeApiClient {

    /**
     * Submit a market order to the exchange
     *
     * @param symbol the symbol for the market
     * @param size the order size
     * @param side {@link MarketSide}
     */
    void submitMarketOrder(String symbol, BigDecimal size, MarketSide side);

    /**
     * Get active positon for given market
     *
     * @param symbol the symbol for the market
     *
     * @return {@link Optional<Position>}
     */
    Optional<Position> getPosition(String symbol);
}
