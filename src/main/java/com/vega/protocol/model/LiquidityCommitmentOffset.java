package com.vega.protocol.model;

import com.vega.protocol.constant.PeggedReference;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Accessors(chain = true)
public class LiquidityCommitmentOffset {
    private BigInteger proportion;
    private BigDecimal offset;
    private PeggedReference reference;
}