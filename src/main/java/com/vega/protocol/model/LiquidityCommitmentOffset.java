package com.vega.protocol.model;

import com.vega.protocol.constant.PeggedReference;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LiquidityCommitmentOffset {
    private long proportion;
    private double offset;
    private PeggedReference reference;
}