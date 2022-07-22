package com.vega.protocol.store;

import java.util.Optional;

public abstract class SingleItemStore<T> {

    private T item;

    /**
     * Update the item in the store
     *
     * @param item {@link T}
     */
    public void update(T item) {
        this.item = item;
    }

    /**
     * Get the item from the store
     *
     * @return {@link Optional<T>}
     */
    public Optional<T> get() {
        if(item == null) return Optional.empty();
        return Optional.of(item);
    }
}