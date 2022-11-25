package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.Position;

import java.util.Optional;

public interface ExchangeApiClient {

    /**
     * Submit a market order to the exchange
     *
     * @param symbol the symbol for the market
     * @param size the order size
     * @param side {@link MarketSide}
     */
    void submitMarketOrder(String symbol, double size, MarketSide side);

    /**
     * Get active position for given market
     *
     * @param symbol the symbol for the market
     *
     * @return {@link Optional<Position>}
     */
    Optional<Position> getPosition(String symbol);
}
