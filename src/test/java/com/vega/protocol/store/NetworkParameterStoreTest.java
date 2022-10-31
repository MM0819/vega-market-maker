package com.vega.protocol.store;

import com.vega.protocol.model.NetworkParameter;

public class NetworkParameterStoreTest extends MultipleItemStoreTest<NetworkParameter, NetworkParameterStore> {

    private final NetworkParameterStore store = new NetworkParameterStore();

    @Override
    public NetworkParameterStore getStore() {
        return store;
    }

    @Override
    public NetworkParameter getItem() {
        return new NetworkParameter();
    }
}
