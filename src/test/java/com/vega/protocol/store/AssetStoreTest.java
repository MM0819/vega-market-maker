package com.vega.protocol.store;

import com.vega.protocol.model.Asset;
import com.vega.protocol.store.vega.AssetStore;

public class AssetStoreTest extends MultipleItemStoreTest<Asset, AssetStore> {

    private final AssetStore store = new AssetStore();

    @Override
    public AssetStore getStore() {
        return store;
    }

    @Override
    public Asset getItem() {
        return new Asset();
    }
}
