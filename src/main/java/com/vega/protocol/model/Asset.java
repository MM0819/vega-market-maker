package com.vega.protocol.model;

import com.vega.protocol.constant.AssetStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Asset extends UniqueItem {
    private String id;
    private String name;
    private String symbol;
    private int decimalPlaces;
    private double quantum;
    private AssetStatus status;
}