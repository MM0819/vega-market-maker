package com.vega.protocol.utils;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.model.trading.DistributionStep;
import org.springframework.stereotype.Component;
import vega.Markets;
import vega.Vega;
import vega.commands.v1.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PricingUtils {

    private final DecimalUtils decimalUtils;

    public PricingUtils(DecimalUtils decimalUtils) {
        this.decimalUtils = decimalUtils;
    }

    /**
     * Build the distribution for a given mid-price, target volume and range
     *
     * @param midPrice this will be the best bid or ask
     * @param totalVolume the total volume across all quotes
     * @param range the depth for the quotes
     * @param side {@link MarketSide}
     * @param orderCount the number of orders to return
     *
     * @return {@link List<DistributionStep>}
     */
    public List<DistributionStep> getDistribution(
            final double midPrice,
            final double totalVolume,
            final double range,
            final MarketSide side,
            final int orderCount
    ) {
        List<DistributionStep> distribution = new ArrayList<>();
        double iter = 6.0 / (double) orderCount;
        double adjustment = Math.pow(3, 1.0 / 3.0);
        double total_size = 0;
        for(double x=-3; x<=3; x+=iter) {
            double y = Math.pow(Math.abs(x), 1.0 / 3.0);
            if(x < 0) {
                y = y * -1.0;
            }
            double offset = ((((x + iter) + 3) / 6) * range * midPrice);
            double price = side.equals(MarketSide.SELL) ? midPrice + offset : midPrice - offset;
            double size = (y + adjustment) - total_size;
            distribution.add(new DistributionStep()
                    .setPrice(Math.round(price * 10000.0) / 10000.0)
                    .setSize(Math.round(size * 10000.0) / 10000.0));
            total_size += size;
        }
        double sum = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        double modifier = totalVolume / sum;
        distribution.forEach(d -> d.setSize(d.getSize() * modifier));
        return distribution;
    }

    /**
     * Applies the target spread to bids and asks
     *
     * @param bestBidPrice the best bid price
     * @param bestAskPrice the best ask price
     * @param makerFee the maker fee on the market
     * @param market {@link Markets.Market}
     * @param marketConfig {@link MarketConfig}
     */
    public double getSpreadDelta(
            final double bestBidPrice,
            final double bestAskPrice,
            final double makerFee,
            final Markets.Market market,
            final MarketConfig marketConfig
    ) {
        double hedgeSpread = (bestAskPrice - bestBidPrice) / bestBidPrice;
        double liquidityFee = Double.parseDouble(market.getFees().getFactors().getLiquidityFee());
        double liquidityRebate = Math.min(liquidityFee, liquidityFee * -1);
        double makerRebate = Math.min(makerFee, makerFee * -1);
        double targetSpread = getTargetSpread(hedgeSpread, marketConfig.getTargetEdge(),
                marketConfig.getHedgeFee(), liquidityRebate, makerRebate);
        double currentSpread = (bestAskPrice - bestBidPrice) / bestBidPrice;
        double delta = 0;
        if(currentSpread < targetSpread) {
            delta = (targetSpread - currentSpread) * bestBidPrice * 0.5;
        }
        return delta;
    }

    /**
     * Get the target spread based on efficiency of hedging exposure
     *
     * @param hedgeSpread the spread on the hedging market
     * @param targetEdge the edge that we want to target
     * @param hedgeFee the fee on the hedging market
     * @param liquidityRebate the liquidity rebate on Vega
     * @param makerRebate the maker rebate on Vega
     *
     * @return the spread for Vega quotes
     */
    private double getTargetSpread(
            final double hedgeSpread,
            final double targetEdge,
            final double hedgeFee,
            final double liquidityRebate,
            final double makerRebate
    ) {
        return targetEdge + hedgeFee + liquidityRebate + makerRebate + hedgeSpread;
    }

    /**
     * Get the target volume for side of book based on balance and exposure
     *
     * @param side {@link MarketSide}
     * @param balance user's balance
     * @param exposure user's exposure
     * @param midPrice the mid-price
     * @param tradingConfig {@link TradingConfig}
     *
     * @return target notional volume
     */
    public double getTargetVolume(
            final MarketSide side,
            final double balance,
            final double exposure,
            final double midPrice,
            final TradingConfig tradingConfig
    ) {
        double askPoolSize = balance * 0.5 / midPrice;
        double volume = askPoolSize * tradingConfig.getCommitmentBalanceRatio();
        double openVolumeRatio = Math.min(0.99, Math.abs(exposure) / askPoolSize);
        if((exposure > 0 && side.equals(MarketSide.BUY)) || (exposure < 0 && side.equals(MarketSide.SELL))) {
            volume = volume * (1 - openVolumeRatio);
        }
        return volume;
    }

    /**
     * Convert {@link DistributionStep}s to {@link Commands.OrderSubmission}s
     *
     * @param distribution {@link List<DistributionStep>}
     * @param side {@link Vega.Side}
     * @param sizeScalingFactor scaling factor for order sizes
     * @param market {@link vega.Markets.Market}
     *
     * @return {@link List<Commands.OrderSubmission>}
     */
    public List<Commands.OrderSubmission> distributionToOrders(
            final List<DistributionStep> distribution,
            final Vega.Side side,
            final Double sizeScalingFactor,
            final Markets.Market market
    ) {
        return distribution.stream().map(d -> {
            double size = d.getSize() * sizeScalingFactor;
            return Commands.OrderSubmission.newBuilder()
                    .setSize(decimalUtils.convertFromDecimals(market.getPositionDecimalPlaces(), size).longValue())
                    .setPrice(decimalUtils.convertFromDecimals(market.getDecimalPlaces(), d.getPrice())
                            .toBigInteger().toString())
                    .setSide(side)
                    .setType(Vega.Order.Type.TYPE_LIMIT)
                    .setTimeInForce(Vega.Order.TimeInForce.TIME_IN_FORCE_GTC)
                    .setMarketId(market.getId())
                    .build();
        }).collect(Collectors.toList());
    }
}