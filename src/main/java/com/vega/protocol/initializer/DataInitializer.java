package com.vega.protocol.initializer;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final OrderStore orderStore;
    private final MarketStore marketStore;
    private final VegaApiClient vegaApiClient;
    private final String partyId;

    public DataInitializer(OrderStore orderStore,
                           MarketStore marketStore,
                           VegaApiClient vegaApiClient,
                           @Value("${vega.party.id}") String partyId) {
        this.orderStore = orderStore;
        this.marketStore = marketStore;
        this.vegaApiClient = vegaApiClient;
        this.partyId = partyId;
    }

    public void initialize() {
        vegaApiClient.getMarkets().forEach(marketStore::add);
        vegaApiClient.getOpenOrders(partyId).forEach(orderStore::add);
    }
}