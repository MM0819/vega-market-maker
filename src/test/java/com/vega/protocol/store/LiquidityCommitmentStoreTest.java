package com.vega.protocol.store;

import com.vega.protocol.model.LiquidityCommitment;

public class LiquidityCommitmentStoreTest extends MultipleItemStoreTest<LiquidityCommitment, LiquidityCommitmentStore> {

    private final LiquidityCommitmentStore store = new LiquidityCommitmentStore();

    @Override
    public LiquidityCommitmentStore getStore() {
        return store;
    }

    @Override
    public LiquidityCommitment getItem() {
        return new LiquidityCommitment();
    }
}
