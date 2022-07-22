package com.vega.protocol.store;

import com.vega.protocol.model.UniqueItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class MultipleItemStoreTest<X extends UniqueItem, T extends MultipleItemStore<X>> {

    public abstract T getStore();
    public abstract X getItem();

    @Test
    public void testAddAndUpdateAndRemove() {
        X item = getItem();
        item.setId("1");
        getStore().add(item);
        Assertions.assertEquals(getStore().getItems().size(), 1);
        getStore().add(item);
        Assertions.assertEquals(getStore().getItems().size(), 1);
        getStore().update(item);
        Assertions.assertEquals(getStore().getItems().size(), 1);
        getStore().remove(item);
        Assertions.assertEquals(getStore().getItems().size(), 0);
    }
}