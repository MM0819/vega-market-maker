package com.vega.protocol.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Market extends UniqueItem {
    private String id;
    private String name;
    private String status;
    private String settlementAsset;
    private int decimalPlaces;
}