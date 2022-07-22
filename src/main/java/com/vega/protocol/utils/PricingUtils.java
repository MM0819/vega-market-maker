package com.vega.protocol.utils;

import com.vega.protocol.model.DistributionStep;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class PricingUtils {

    private PricingUtils() {}

    /**
     * Calculates the bid size for a given trade size and scaling factor
     *
     * @param askSize the trade size (aggressive ask)
     * @param askPoolSize the size of the ask pool
     * @param bidPoolSize the size of the bid pool
     * @param scalingFactor the scaling factor (between 0 and 1), where 0 = more slippage
     *
     * @return the bid asset received for the ask size
     */
    public static double getBidSize(
            final double askSize,
            final double askPoolSize,
            final double bidPoolSize,
            final double scalingFactor
    ) {
        return bidPoolSize * (Math.pow((1 + (askSize / askPoolSize)), (1 / scalingFactor)) - 1);
    }

    /**
     * Calculates the ask size for a given trade size and scaling factor
     *
     * @param bidSize the trade size (aggressive bid)
     * @param askPoolSize the size of the ask pool
     * @param bidPoolSize the size of the bid pool
     * @param scalingFactor the scaling factor (between 1 and infinity), where infinity = more slippage
     *
     * @return the ask asset received for the bid size
     */
    public static double getAskSize(
            final double bidSize,
            final double askPoolSize,
            final double bidPoolSize,
            final double scalingFactor
    ) {
        return askPoolSize * (Math.pow((1 + (bidSize / bidPoolSize)), scalingFactor) - 1);
    }

    /**
     * Returns the distribution of the bid
     *
     * @param bidPoolSize the size of the bid pool
     * @param askPoolSize the size of the ask pool
     * @param askSize the initial ask size (first aggressive trade)
     * @param priceCutOff the price at which the distribution is complete
     * @param stepSize increase the ask size by this much on each iteration (should be a %, e.g. 0.1 = 10%)
     * @param scalingFactor the scaling factor used, which shifts the bids further out from the mid-price
     * @param orderCount the target order count
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public static List<DistributionStep> getBidDistribution(
            double bidPoolSize,
            double askPoolSize,
            double askSize,
            double priceCutOff,
            double stepSize,
            double scalingFactor,
            int orderCount
    ) {
        double price = bidPoolSize / askPoolSize;
        List<DistributionStep> distribution = new ArrayList<>();
        while(price >= priceCutOff) {
            double bidSize = getBidSize(askSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize -= bidSize;
            askPoolSize += askSize;
            price = bidPoolSize / askPoolSize;
            askSize = askSize * (1 + stepSize);
        }
        if(distribution.size() > orderCount) {
            distribution = aggregateDistribution(distribution, orderCount);
        }
        return distribution;
    }

    /**
     * Returns the distribution of the ask
     *
     * @param bidPoolSize the size of the bid pool
     * @param askPoolSize the size of the ask pool
     * @param bidSize the initial bid size (first aggressive trade)
     * @param priceCutOff the price at which the distribution is complete
     * @param stepSize increase the bid size by this much on each iteration (should be a %, e.g. 0.1 = 10%)
     * @param scalingFactor the scaling factor used, which shifts the asks further out from the mid-price
     * @param orderCount the target order count
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public static List<DistributionStep> getAskDistribution(
            double bidPoolSize,
            double askPoolSize,
            double bidSize,
            double priceCutOff,
            double stepSize,
            double scalingFactor,
            int orderCount
    ) {
        double price = bidPoolSize / askPoolSize;
        List<DistributionStep> distribution = new ArrayList<>();
        while(price <= priceCutOff) {
            double askSize = getAskSize(bidSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize += bidSize;
            askPoolSize -= askSize;
            price = bidPoolSize / askPoolSize;
            bidSize = bidSize * (1 + stepSize);
        }
        if(distribution.size() > orderCount) {
            distribution = aggregateDistribution(distribution, orderCount);
        }
        return distribution;
    }

    /**
     * Aggregate the orders in the distribution
     *
     * @param distribution {@link List<DistributionStep>}
     * @param orderCount the number of orders
     *
     * @return {@link List<DistributionStep>}
     */
    private static List<DistributionStep> aggregateDistribution(
            List<DistributionStep> distribution,
            int orderCount
    ) {
        List<Double> prices = distribution.stream().map(DistributionStep::getPrice).collect(Collectors.toList());
        List<Double> sizes = distribution.stream().map(DistributionStep::getSize).collect(Collectors.toList());
        int partitionSize = prices.size() / orderCount;
        List<List<Double>> pricePartitions = ListUtils.partition(prices, partitionSize);
        List<List<Double>> sizePartitions = ListUtils.partition(sizes, partitionSize);
        distribution = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            distribution.add(new DistributionStep()
                    .setPrice(pricePartitions.get(i).get(partitionSize - 1))
                    .setSize(sizePartitions.get(i).stream().mapToDouble(Double::doubleValue).sum()));
        }
        return distribution;
    }

    /**
     * Get the bid scaling factor, used to push bids out from the mid price
     *
     * @param openVolume the current open volume of our party
     * @param openVolumeRatio the % of total collateral allocated to our open volume
     *
     * @return the bid scaling factor (a number between 0 and 1)
     */
    public static double getBidScalingFactor(
            final long openVolume,
            final double openVolumeRatio
    ) {
        return openVolume < 0 ? 1 - Math.abs(openVolumeRatio) : 1;
    }

    /**
     * Get the ask scaling factor, used to push asks out from the mid-price
     *
     * @param openVolume the current open volume of our party
     * @param openVolumeRatio the % of total collateral allocated to our open volume
     *
     * @return the ask scaling factor (a number between 1 and infinity)
     */
    public static double getAskScalingFactor(
            final long openVolume,
            final double openVolumeRatio
    ) {
        return openVolume > 0 ? 1 / (1 - openVolumeRatio) : 1;
    }

    /**
     * Get the bid distribution
     *
     * @param scalingFactor scaling factor, used to push bids out from mid-price
     * @param bidPoolSize the size of our bid pool (i.e. total collateral)
     * @param askPoolSize the size of our ask pool (derived from bid pool and reference price)
     * @param bidQuoteRange the depth of bids
     * @param orderCount the number of orders
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public static List<DistributionStep> getBidDistribution(
            final double scalingFactor,
            final double bidPoolSize,
            final double askPoolSize,
            final double bidQuoteRange,
            final int orderCount
    ) {
        double price = bidPoolSize / askPoolSize;
        double cutoff = price * (1 - bidQuoteRange);
        return PricingUtils.getBidDistribution(bidPoolSize, askPoolSize, 1 / price,
                cutoff, 0.1, scalingFactor, orderCount);
    }

    /**
     * Get the ask distribution
     *
     * @param scalingFactor scaling factor, used to push asks out from mid-price
     * @param bidPoolSize the size of our bid pool (i.e. total collateral)
     * @param askPoolSize the size of our ask pool (derived from bid pool and reference price)
     * @param askQuoteRange the depth of asks
     * @param orderCount the number of orders
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public static List<DistributionStep> getAskDistribution(
            final double scalingFactor,
            final double bidPoolSize,
            final double askPoolSize,
            final double askQuoteRange,
            final int orderCount
    ) {
        double cutoff = (bidPoolSize / askPoolSize) * (1 + askQuoteRange);
        return PricingUtils.getAskDistribution(bidPoolSize, askPoolSize, 1,
                cutoff, 0.1, scalingFactor, orderCount);
    }
}