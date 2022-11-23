package com.vega.protocol.model;

import com.vega.protocol.constant.LiquidityCommitmentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class LiquidityCommitment extends UniqueItem {
    private String id;
    private Market market;
    private String partyId;
    private double fee;
    private double commitmentAmount;
    private List<LiquidityCommitmentOffset> bids = new ArrayList<>();
    private List<LiquidityCommitmentOffset> asks = new ArrayList<>();
    private LiquidityCommitmentStatus status;
}