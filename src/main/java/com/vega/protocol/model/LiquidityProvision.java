package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class LiquidityProvision {
    private BigDecimal fee;
    private BigDecimal commitment;
    private List<LiquidityProvisionOffset> bids = new ArrayList<>();
    private List<LiquidityProvisionOffset> asks = new ArrayList<>();
}