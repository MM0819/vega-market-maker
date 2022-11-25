package com.vega.protocol.store;

import com.vega.protocol.model.trading.ReferencePrice;

public class ReferencePriceStoreTest extends SingleItemStoreTest<ReferencePrice, ReferencePriceStore> {

    private final ReferencePriceStore store = new ReferencePriceStore();

    @Override
    public ReferencePriceStore getStore() {
        return store;
    }

    @Override
    public ReferencePrice getItem() {
        return new ReferencePrice();
    }
}
