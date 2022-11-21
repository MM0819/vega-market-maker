package com.vega.protocol.entity;

import com.vega.protocol.constant.ReferencePriceSource;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "market_config")
@Accessors(chain = true)
public class MarketConfig {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String marketId;
    @Column(nullable = false)
    private String partyId;
    @Column(nullable = false)
    private BigDecimal allocatedMargin;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferencePriceSource referencePriceSource;
    @Column(nullable = false)
    private String referencePriceSymbol;
    @Column(nullable = false)
    private String hedgeSymbol;
    @Column(nullable = false)
    private Integer updateQuotesFrequency;
    @Column(nullable = false)
    private Integer updateLiquidityCommitmentFrequency;
    @Column(nullable = false)
    private Integer updateHedgeFrequency;
    @Column(nullable = false)
    private Integer updateNaiveFlowFrequency;
    @Column(nullable = false)
    private Boolean updateQuotesEnabled;
    @Column(nullable = false)
    private Boolean updateLiquidityCommitmentEnabled;
    @Column(nullable = false)
    private Boolean updateHedgeEnabled;
    @Column(nullable = false)
    private Boolean updateNaiveFlowEnabled;
    @Column(nullable = false)
    private Double hedgeFee;
    @Column(nullable = false)
    private Double targetEdge;
}