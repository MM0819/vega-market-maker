package com.vega.protocol.store;

import datanode.api.v2.TradingData;
import lombok.Getter;
import org.springframework.stereotype.Repository;
import vega.Assets;
import vega.Markets;
import vega.Vega;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class VegaStore {
    @Getter
    private final List<Markets.Market> markets = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Vega.MarketData> marketsData = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Assets.Asset> assets = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Vega.Position> positions = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<TradingData.AccountBalance> accounts = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Vega.NetworkParameter> networkParameters = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Vega.Order> orders = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private final List<Vega.LiquidityProvision> liquidityProvisions = Collections.synchronizedList(new ArrayList<>());

    public void addAsset(
            final Assets.Asset asset
    ) {
        if(getAssetById(asset.getId()).isEmpty()) {
            synchronized (assets) {
                assets.add(asset);
            }
        }
    }

    public Optional<Assets.Asset> getAssetById(
            final String id
    ) {
        return assets.stream().filter(x -> x.getId().equals(id)).findFirst();
    }

    public void removeAsset(
            final String id
    ) {
        synchronized (assets) {
            assets.removeIf(x -> x.getId().equals(id));
        }
    }

    public void updateAsset(
            final Assets.Asset asset
    ) {
        removeAsset(asset.getId());
        addAsset(asset);
    }

    public void addAccount(
            final TradingData.AccountBalance account
    ) {
        if(getAccountByAssetAndType(account.getAsset(), account.getType()).isEmpty()) {
            synchronized (accounts) {
                accounts.add(account);
            }
        }
    }

    public Optional<TradingData.AccountBalance> getAccountByAssetAndType(
            final String asset,
            final Vega.AccountType type
    ) {
        return accounts.stream().filter(x -> x.getAsset().equals(asset) && x.getType().equals(type)).findFirst();
    }

    public List<TradingData.AccountBalance> getAccountsByAsset(
            final String asset
    ) {
        return accounts.stream().filter(x -> x.getAsset().equals(asset)).toList();
    }

    public void removeAccount(
            final String asset,
            final Vega.AccountType type
    ) {
        synchronized (accounts) {
            accounts.removeIf(x -> x.getAsset().equals(asset) && x.getType().equals(type));
        }
    }

    public void updateAccount(
            final TradingData.AccountBalance account
    ) {
        removeAccount(account.getAsset(), account.getType());
        addAccount(account);
    }

    public void addMarket(
            final Markets.Market market
    ) {
        if(getMarketById(market.getId()).isEmpty()) {
            synchronized (markets) {
                markets.add(market);
            }
        }
    }

    public Optional<Markets.Market> getMarketById(
            final String id
    ) {
        return markets.stream().filter(x -> x.getId().equals(id)).findFirst();
    }

    public void removeMarket(
            final String id
    ) {
        synchronized (markets) {
            markets.removeIf(x -> x.getId().equals(id));
        }
    }

    public void updateMarket(
            final Markets.Market market
    ) {
        removeMarket(market.getId());
        addMarket(market);
    }

    public void addMarketData(
            final Vega.MarketData marketData
    ) {
        if(getMarketDataById(marketData.getMarket()).isEmpty()) {
            synchronized (marketsData) {
                marketsData.add(marketData);
            }
        }
    }

    public Optional<Vega.MarketData> getMarketDataById(
            final String id
    ) {
        return marketsData.stream().filter(x -> x.getMarket().equals(id)).findFirst();
    }

    public void removeMarketData(
            final String id
    ) {
        synchronized (marketsData) {
            marketsData.removeIf(x -> x.getMarket().equals(id));
        }
    }

    public void updateMarketData(
            final Vega.MarketData marketData
    ) {
        removeMarketData(marketData.getMarket());
        addMarketData(marketData);
    }

    public void addNetworkParameter(
            final Vega.NetworkParameter networkParameter
    ) {
        if(getNetworkParameterByKey(networkParameter.getKey()).isEmpty()) {
            synchronized (networkParameters) {
                networkParameters.add(networkParameter);
            }
        }
    }

    public Optional<Vega.NetworkParameter> getNetworkParameterByKey(
            final String key
    ) {
        return networkParameters.stream().filter(x -> x.getKey().equals(key)).findFirst();
    }

    public void removeNetworkParameter(
            final String key
    ) {
        synchronized (networkParameters) {
            networkParameters.removeIf(x -> x.getKey().equals(key));
        }
    }

    public void updateNetworkParameter(
            final Vega.NetworkParameter networkParameter
    ) {
        removeNetworkParameter(networkParameter.getKey());
        addNetworkParameter(networkParameter);
    }

    public void addLiquidityProvision(
            final Vega.LiquidityProvision liquidityProvision
    ) {
        if(getLiquidityProvisionByMarketIdAndPartyId(
                liquidityProvision.getMarketId(), liquidityProvision.getPartyId()).isEmpty()) {
            synchronized (liquidityProvisions) {
                liquidityProvisions.add(liquidityProvision);
            }
        }
    }

    public void removeLiquidityProvision(
            final String marketId,
            final String partyId
    ) {
        synchronized (liquidityProvisions) {
            liquidityProvisions.removeIf(x -> x.getMarketId().equals(marketId) && x.getPartyId().equals(partyId));
        }
    }

    public void updateLiquidityProvision(
            final Vega.LiquidityProvision liquidityProvision
    ) {
        removeLiquidityProvision(liquidityProvision.getMarketId(), liquidityProvision.getPartyId());
        addLiquidityProvision(liquidityProvision);
    }

    public Optional<Vega.LiquidityProvision> getLiquidityProvisionByMarketIdAndPartyId(
            final String marketId,
            final String partyId
    ) {
        return liquidityProvisions.stream().filter(x -> x.getMarketId().equals(marketId) &&
                x.getPartyId().equals(partyId)).findFirst();
    }

    public List<Vega.Order> getOrdersByMarketIdAndStatus(
            final String marketId,
            final Vega.Order.Status status
    ) {
        return orders.stream().filter(x -> x.getMarketId().equals(marketId) &&
                x.getStatus().equals(status)).collect(Collectors.toList());
    }

    public Optional<Vega.Order> getOrderById(
            final String id
    ) {
        return orders.stream().filter(x -> x.getId().equals(id)).findFirst();
    }

    public void addOrder(
            final Vega.Order order
    ) {
        if(getOrderById(order.getId()).isEmpty()) {
            synchronized (orders) {
                orders.add(order);
            }
        }
    }

    public void removeOrder(
            final String id
    ) {
        synchronized (orders) {
            orders.removeIf(x -> x.getId().equals(id));
        }
    }

    public void updateOrder(
            final Vega.Order order
    ) {
        removeOrder(order.getId());
        addOrder(order);
    }

    public Optional<Vega.Position> getPositionByMarketIdAndPartyId(
            final String marketId,
            final String partyId
    ) {
        return positions.stream().filter(x -> x.getMarketId().equals(marketId) &&
                x.getPartyId().equals(partyId)).findFirst();
    }

    public void addPosition(
            final Vega.Position position
    ) {
        if(getPositionByMarketIdAndPartyId(position.getMarketId(), position.getPartyId()).isEmpty()) {
            synchronized (positions) {
                positions.add(position);
            }
        }
    }

    public void removePosition(
            final String marketId,
            final String partyId
    ) {
        synchronized (positions) {
            positions.removeIf(x -> x.getPartyId().equals(partyId) && x.getMarketId().equals(marketId));
        }
    }

    public void updatePosition(
            final Vega.Position position
    ) {
        removePosition(position.getMarketId(), position.getPartyId());
        addPosition(position);
    }
}
