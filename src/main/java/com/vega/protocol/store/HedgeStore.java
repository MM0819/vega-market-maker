package com.vega.protocol.store;

import com.vega.protocol.model.trading.Hedge;
import org.springframework.stereotype.Repository;

@Repository
public class HedgeStore extends MultipleItemStore<Hedge> {
}