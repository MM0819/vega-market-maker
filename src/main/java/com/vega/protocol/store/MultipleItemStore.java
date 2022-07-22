package com.vega.protocol.store;

import com.vega.protocol.model.UniqueItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class MultipleItemStore<T extends UniqueItem> {

    private final List<T> items = Collections.synchronizedList(new ArrayList<>());

    /**
     * Get an item by ID
     *
     * @param id the unique ID
     *
     * @return {@link Optional<T>}
     */
    public Optional<T> getById(String id) {
        return items.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    /**
     * Add a new item to the store
     *
     * @param item {@link T}
     */
    public void add(T item) {
        synchronized (items) {
            if(getById(item.getId()).isEmpty()) {
                items.add(item);
            }
        }
    }

    /**
     * Remove an item from the store
     *
     * @param item {@link T}
     */
    public void remove(T item) {
        synchronized (items) {
            items.removeIf(i -> i.getId().equals(item.getId()));
        }
    }

    /**
     * Update an item in the store
     *
     * @param item {@link T}
     */
    public void update(T item) {
        remove(item);
        add(item);
    }

    /**
     * Get all items from the store
     *
     * @return {@link List<T>}
     */
    public List<T> getItems() {
        return new ArrayList<>(items);
    }
}