package com.vega.protocol.store;

import com.vega.protocol.model.Asset;
import org.springframework.stereotype.Repository;

@Repository
public class AssetStore extends MultipleItemStore<Asset> {
}