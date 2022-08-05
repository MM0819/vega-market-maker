package com.vega.protocol.store;

import com.vega.protocol.model.AppConfig;

public class AppConfigStoreTest extends SingleItemStoreTest<AppConfig, AppConfigStore> {

    private final AppConfigStore store = new AppConfigStore();

    @Override
    public AppConfigStore getStore() {
        return store;
    }

    @Override
    public AppConfig getItem() {
        return new AppConfig();
    }
}
