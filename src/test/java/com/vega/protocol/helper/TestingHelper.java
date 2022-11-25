package com.vega.protocol.helper;

import datanode.api.v2.TradingData;
import vega.Markets;
import vega.Vega;

public class TestingHelper {

    public static final String ID = "12345";

    public static Markets.Market getMarket(
            final Markets.Market.State state,
            final Markets.Market.TradingMode tradingMode,
            final String settlementAsset
    ) {
        return Markets.Market.newBuilder()
                .setId(ID)
                .setState(state)
                .setTradingMode(tradingMode)
                .setTradableInstrument(Markets.TradableInstrument.newBuilder()
                        .setInstrument(Markets.Instrument.newBuilder()
                                .setFuture(Markets.Future.newBuilder()
                                        .setSettlementAsset(settlementAsset)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public static Vega.MarketData getMarketData(
            final String targetStake,
            final String suppliedStake
    ) {
        return Vega.MarketData.newBuilder()
                .setTargetStake(targetStake)
                .setSuppliedStake(suppliedStake)
                .build();
    }

    public static Vega.LiquidityProvision getLiquidityProvision(
            final String commitmentAmount,
            final Markets.Market market
    ) {
        return Vega.LiquidityProvision.newBuilder()
                .setId(ID)
                .setCommitmentAmount(commitmentAmount)
                .setMarketId(market.getId())
                .build();
    }

    public static Vega.Order getOrder(
            final String id,
            final String price,
            final long size,
            final Vega.Side side,
            final Vega.Order.Status status,
            final String liquidityProvisionId
    ) {
        return Vega.Order.newBuilder()
                .setId(id)
                .setPrice(price)
                .setSize(size)
                .setSide(side)
                .setStatus(status)
                .setLiquidityProvisionId(liquidityProvisionId)
                .build();
    }

    public static TradingData.AccountBalance getAccount(
            final String marketId,
            final String balance,
            final String asset
    ) {
        return TradingData.AccountBalance.newBuilder()
                .setBalance(balance)
                .setAsset(asset)
                .setMarketId(marketId)
                .build();
    }

    public static Vega.Position getPosition(
            final long openVolume,
            final String entryPrice,
            final String marketId,
            final String partyId
    ) {
        return Vega.Position.newBuilder()
                .setAverageEntryPrice(entryPrice)
                .setOpenVolume(openVolume)
                .setPartyId(partyId)
                .setMarketId(marketId)
                .build();
    }
}
