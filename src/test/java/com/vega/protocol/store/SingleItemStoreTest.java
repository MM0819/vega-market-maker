package com.vega.protocol.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class SingleItemStoreTest<X, T extends SingleItemStore<X>> {

    public abstract T getStore();
    public abstract X getItem();

    @Test
    public void testUpdate() {
        X item = getItem();
        Assertions.assertTrue(getStore().get().isEmpty());
        getStore().update(item);
        Assertions.assertTrue(getStore().get().isPresent());
    }
}