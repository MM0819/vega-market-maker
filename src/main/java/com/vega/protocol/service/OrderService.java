package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.store.VegaStore;
import org.springframework.stereotype.Service;
import vega.Vega;

import java.util.List;

@Service
public class OrderService {

    private final VegaStore vegaStore;

    public OrderService(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    public List<Vega.Order> getByMarketIdAndStatus(
            final String marketId,
            final Vega.Order.Status status
    ) {
        return vegaStore.getOrdersByMarketIdAndStatus(marketId, status);
    }

    /**
     * Get the opposite {@link MarketSide}
     *
     * @param side {@link MarketSide}
     *
     * @return opposite {@link MarketSide}
     */
    public Vega.Side getOtherSide(
            Vega.Side side
    ) {
        return side.equals(Vega.Side.SIDE_BUY) ? Vega.Side.SIDE_SELL : Vega.Side.SIDE_BUY;
    }
}