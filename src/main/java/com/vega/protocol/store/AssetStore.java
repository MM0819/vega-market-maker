package com.vega.protocol.store;

import com.vega.protocol.model.Asset;
import com.vega.protocol.store.MultipleItemStore;
import org.springframework.stereotype.Repository;

@Repository
public class AssetStore extends MultipleItemStore<Asset> {
}