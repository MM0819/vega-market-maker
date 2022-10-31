package com.vega.protocol.store;

import com.vega.protocol.model.Hedge;

public class HedgeStoreTest extends MultipleItemStoreTest<Hedge, HedgeStore> {

    private final HedgeStore store = new HedgeStore();

    @Override
    public HedgeStore getStore() {
        return store;
    }

    @Override
    public Hedge getItem() {
        return new Hedge();
    }
}
