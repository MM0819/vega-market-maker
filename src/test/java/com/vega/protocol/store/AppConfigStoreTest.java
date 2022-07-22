package com.vega.protocol.store;

import com.vega.protocol.model.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testInitialize() {
        store.initialize();
        Assertions.assertTrue(getStore().get().isPresent());
    }
}
