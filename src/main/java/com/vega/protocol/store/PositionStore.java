package com.vega.protocol.store;

import com.vega.protocol.model.Position;
import com.vega.protocol.store.MultipleItemStore;
import org.springframework.stereotype.Repository;

@Repository
public class PositionStore extends MultipleItemStore<Position> {
}