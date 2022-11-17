package com.vega.protocol.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Table(name = "trading_config")
@Accessors(chain = true)
public class TradingConfig {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private MarketConfig marketConfig;
    @Column(nullable = false)
    private Double bboOffset;
    @Column(nullable = false)
    private Double bidQuoteRange;
    @Column(nullable = false)
    private Double askQuoteRange;
    @Column(nullable = false)
    private Double bidSizeFactor;
    @Column(nullable = false)
    private Double askSizeFactor;
    @Column(nullable = false)
    private Double commitmentBalanceRatio;
    @Column(nullable = false)
    private Double commitmentSpread;
    @Column(nullable = false)
    private Integer commitmentOrderCount;
    @Column(nullable = false)
    private Integer quoteOrderCount;
    @Column(nullable = false)
    private Double minSpread;
    @Column(nullable = false)
    private Double maxSpread;
    @Column(nullable = false)
    private Double fee;
    @Column(nullable = false)
    private Double stakeBuffer;
}