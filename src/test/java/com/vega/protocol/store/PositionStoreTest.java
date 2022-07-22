package com.vega.protocol.store;

import com.vega.protocol.model.Position;

public class PositionStoreTest extends MultipleItemStoreTest<Position, PositionStore> {

    private final PositionStore store = new PositionStore();

    @Override
    public PositionStore getStore() {
        return store;
    }

    @Override
    public Position getItem() {
        return new Position();
    }
}
