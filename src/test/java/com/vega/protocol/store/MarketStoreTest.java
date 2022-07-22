package com.vega.protocol.store;

import com.vega.protocol.model.Market;

public class MarketStoreTest extends MultipleItemStoreTest<Market, MarketStore> {

    private final MarketStore store = new MarketStore();

    @Override
    public MarketStore getStore() {
        return store;
    }

    @Override
    public Market getItem() {
        return new Market();
    }
}
