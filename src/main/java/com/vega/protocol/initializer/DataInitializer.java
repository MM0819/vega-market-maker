package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.entity.GlobalConfig;
import com.vega.protocol.repository.GlobalConfigRepository;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.store.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer {

    private final OrderStore orderStore;
    private final MarketStore marketStore;
    private final PositionStore positionStore;
    private final GlobalConfigRepository globalConfigRepository;
    private final MarketConfigRepository marketConfigRepository;
    private final AccountStore accountStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final AssetStore assetStore;
    private final NetworkParameterStore networkParameterStore;
    private final VegaApiClient vegaApiClient;
    private final String binanceApiKey;
    private final String binanceApiSecret;
    private final String binanceWebSocketUrl;
    private final Boolean binanceWebSocketEnabled;
    private final String igApiKey;
    private final String igUsername;
    private final String igPassword;
    private final String polygonApiKey;
    private final String polygonWebSocketUrl;
    private final Boolean polygonWebSocketEnabled;
    private final String vegaApiUrl;
    private final String vegaWalletUrl;
    private final String vegaWebSocketUrl;
    private final String vegaWalletUser;
    private final String vegaWalletPassword;
    private final Boolean vegaWebSocketEnabled;
    private final String naiveFlowPartyId;

    @Getter
    private boolean initialized = false;

    public DataInitializer(OrderStore orderStore,
                           MarketStore marketStore,
                           PositionStore positionStore,
                           GlobalConfigRepository globalConfigRepository,
                           MarketConfigRepository marketConfigRepository,
                           AccountStore accountStore,
                           LiquidityCommitmentStore liquidityCommitmentStore,
                           AssetStore assetStore,
                           NetworkParameterStore networkParameterStore,
                           VegaApiClient vegaApiClient,
                           @Value("${binance.api.key}") String binanceApiKey,
                           @Value("${binance.api.secret}") String binanceApiSecret,
                           @Value("${binance.ws.url}") String binanceWebSocketUrl,
                           @Value("${binance.ws.enabled}") Boolean binanceWebSocketEnabled,
                           @Value("${ig.api.key}") String igApiKey,
                           @Value("${ig.username}") String igUsername,
                           @Value("${ig.password}") String igPassword,
                           @Value("${polygon.api.key}") String polygonApiKey,
                           @Value("${polygon.ws.url}") String polygonWebSocketUrl,
                           @Value("${polygon.ws.enabled}") Boolean polygonWebSocketEnabled,
                           @Value("${vega.api.url}") String vegaApiUrl,
                           @Value("${vega.wallet.url}") String vegaWalletUrl,
                           @Value("${vega.ws.url}") String vegaWebSocketUrl,
                           @Value("${vega.wallet.user}") String vegaWalletUser,
                           @Value("${vega.wallet.password}") String vegaWalletPassword,
                           @Value("${vega.ws.enabled}") Boolean vegaWebSocketEnabled,
                           @Value("${naive.flow.party.id}") String naiveFlowPartyId) {
        this.orderStore = orderStore;
        this.marketStore = marketStore;
        this.positionStore = positionStore;
        this.globalConfigRepository = globalConfigRepository;
        this.marketConfigRepository = marketConfigRepository;
        this.accountStore = accountStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.assetStore = assetStore;
        this.networkParameterStore = networkParameterStore;
        this.vegaApiClient = vegaApiClient;
        this.binanceApiKey = binanceApiKey;
        this.binanceApiSecret = binanceApiSecret;
        this.binanceWebSocketUrl = binanceWebSocketUrl;
        this.binanceWebSocketEnabled = binanceWebSocketEnabled;
        this.igApiKey = igApiKey;
        this.igUsername = igUsername;
        this.igPassword = igPassword;
        this.polygonApiKey = polygonApiKey;
        this.polygonWebSocketUrl = polygonWebSocketUrl;
        this.polygonWebSocketEnabled = polygonWebSocketEnabled;
        this.vegaApiUrl = vegaApiUrl;
        this.vegaWalletUrl = vegaWalletUrl;
        this.vegaWebSocketUrl = vegaWebSocketUrl;
        this.vegaWalletUser = vegaWalletUser;
        this.vegaWalletPassword = vegaWalletPassword;
        this.vegaWebSocketEnabled = vegaWebSocketEnabled;
        this.naiveFlowPartyId = naiveFlowPartyId;
    }

    /**
     * Initialize data
     */
    public void initialize() {
        if(globalConfigRepository.findById(1L).isEmpty()) {
            GlobalConfig globalConfig = new GlobalConfig()
                    .setId(1L)
                    .setBinanceApiKey(binanceApiKey)
                    .setBinanceApiSecret(binanceApiSecret)
                    .setBinanceWebSocketUrl(binanceWebSocketUrl)
                    .setBinanceWebSocketEnabled(binanceWebSocketEnabled)
                    .setIgApiKey(igApiKey)
                    .setIgUsername(igUsername)
                    .setIgPassword(igPassword)
                    .setPolygonApiKey(polygonApiKey)
                    .setPolygonWebSocketUrl(polygonWebSocketUrl)
                    .setPolygonWebSocketEnabled(polygonWebSocketEnabled)
                    .setVegaApiUrl(vegaApiUrl)
                    .setVegaWalletUrl(vegaWalletUrl)
                    .setVegaWebSocketUrl(vegaWebSocketUrl)
                    .setVegaWalletUser(vegaWalletUser)
                    .setVegaWalletPassword(vegaWalletPassword)
                    .setVegaWebSocketEnabled(vegaWebSocketEnabled)
                    .setNaiveFlowPartyId(naiveFlowPartyId);
            globalConfigRepository.save(globalConfig);
        }
        updateState();
        initialized = true;
    }

    private void updateState() {
        vegaApiClient.getNetworkParameters().forEach(networkParameterStore::update);
        vegaApiClient.getAssets().forEach(assetStore::update);
        vegaApiClient.getMarkets().forEach(marketStore::update);
        marketConfigRepository.findAll().forEach(marketConfig -> {
            vegaApiClient.getAccounts(marketConfig.getPartyId()).forEach(accountStore::update);
            vegaApiClient.getPositions(marketConfig.getPartyId()).forEach(positionStore::update);
            vegaApiClient.getOpenOrders(marketConfig.getPartyId()).forEach(orderStore::update);
            vegaApiClient.getLiquidityCommitments(marketConfig.getPartyId()).forEach(liquidityCommitmentStore::update);
        });
    }
}