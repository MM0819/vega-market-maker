package com.vega.protocol.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "global_config")
@Accessors(chain = true)
public class GlobalConfig {
    @Id
    private Long id;
    @Column(nullable = false)
    private String naiveFlowPartyId;
    @Column(nullable = false)
    private String vegaWebSocketUrl;
    @Column(nullable = false)
    private String vegaApiUrl;
    @Column(nullable = false)
    private String vegaWalletUrl;
    @Column(nullable = false)
    private String polygonWebSocketUrl;
    @Column(nullable = false)
    private String binanceWebSocketUrl;
    @Column(nullable = false)
    private Boolean polygonWebSocketEnabled;
    @Column(nullable = false)
    private Boolean binanceWebSocketEnabled;
    @Column(nullable = false)
    private Boolean vegaWebSocketEnabled;
    @Column(nullable = false)
    private String polygonApiKey;
    @Column(nullable = false)
    private String igApiKey;
    @Column(nullable = false)
    private String igUsername;
    @Column(nullable = false)
    private String igPassword;
    @Column(nullable = false)
    private String binanceApiKey;
    @Column(nullable = false)
    private String binanceApiSecret;
    @Column(nullable = false)
    private String vegaWalletUser;
    @Column(nullable = false)
    private String vegaWalletPassword;
}