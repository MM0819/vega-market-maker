package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LiquidityProvisionOffset {
    Long portion;
    Long offset;
}