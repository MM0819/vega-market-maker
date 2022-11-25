package com.vega.protocol.initializer;

import com.vega.protocol.entity.GlobalConfig;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.repository.GlobalConfigRepository;
import com.vega.protocol.repository.MarketConfigRepository;
import com.vega.protocol.store.VegaStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataInitializer {

    private final GlobalConfigRepository globalConfigRepository;
    private final MarketConfigRepository marketConfigRepository;
    private final VegaStore vegaStore;
    private final VegaGrpcClient vegaGrpcClient;
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

    public DataInitializer(VegaStore vegaStore,
                           GlobalConfigRepository globalConfigRepository,
                           MarketConfigRepository marketConfigRepository,
                           VegaGrpcClient vegaGrpcClient,
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
        this.vegaStore = vegaStore;
        this.globalConfigRepository = globalConfigRepository;
        this.marketConfigRepository = marketConfigRepository;
        this.vegaGrpcClient = vegaGrpcClient;
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
        initializeState();
        initialized = true;
    }

    /**
     * Initialize gRPC streams
     */
    private void initializeStreaming() {
        marketConfigRepository.findAll().forEach(marketConfig -> {
            String partyId = marketConfig.getPartyId();
            String marketId = marketConfig.getMarketId();
            vegaGrpcClient.streamAccounts(partyId, (items) -> items.stream()
                    .filter(i -> i.getMarketId().equals(marketId)).forEach(vegaStore::updateAccount));
            vegaGrpcClient.streamPositions(partyId, (items) -> items.stream()
                    .filter(i -> i.getMarketId().equals(marketId)).forEach(vegaStore::updatePosition));
            vegaGrpcClient.streamOrders(partyId, (items) -> items.stream()
                    .filter(i -> i.getMarketId().equals(marketId)).forEach(vegaStore::updateOrder));
            vegaGrpcClient.streamLiquidityProvisions(partyId, (items) -> items.stream()
                    .filter(i -> i.getMarketId().equals(marketId)).forEach(vegaStore::updateLiquidityProvision));
            vegaGrpcClient.streamMarketData(List.of(marketId), (items) -> items.forEach(vegaStore::updateMarketData));
        });
    }

    /**
     * Initialize state
     */
    private void initializeState() {
        vegaGrpcClient.getNetworkParameters().forEach(vegaStore::updateNetworkParameter);
        vegaGrpcClient.getAssets().forEach(vegaStore::updateAsset);
        vegaGrpcClient.getMarkets().forEach(vegaStore::updateMarket);
        marketConfigRepository.findAll().forEach(marketConfig -> {
            vegaGrpcClient.getAccounts(List.of(marketConfig.getPartyId()))
                    .forEach(vegaStore::updateAccount);
            vegaGrpcClient.getPositions(marketConfig.getPartyId(), marketConfig.getMarketId())
                    .forEach(vegaStore::updatePosition);
            vegaGrpcClient.getOrders(marketConfig.getPartyId(), marketConfig.getMarketId(), true)
                    .forEach(vegaStore::updateOrder);
            vegaGrpcClient.getLiquidityProvisions(marketConfig.getPartyId(), marketConfig.getMarketId())
                    .forEach(vegaStore::updateLiquidityProvision);
        });
        initializeStreaming();
    }
}