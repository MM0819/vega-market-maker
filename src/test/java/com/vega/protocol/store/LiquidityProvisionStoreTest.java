package com.vega.protocol.store;

import com.vega.protocol.model.LiquidityProvision;

public class LiquidityProvisionStoreTest extends SingleItemStoreTest<LiquidityProvision, LiquidityProvisionStore> {

    private final LiquidityProvisionStore store = new LiquidityProvisionStore();

    @Override
    public LiquidityProvisionStore getStore() {
        return store;
    }

    @Override
    public LiquidityProvision getItem() {
        return new LiquidityProvision();
    }
}
