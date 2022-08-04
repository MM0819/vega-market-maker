package com.vega.protocol.utils;

import com.vega.protocol.model.DistributionStep;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PricingUtils {

    private static final double STEP_SIZE = 0.1;

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
    private double getBidSize(
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
    private double getAskSize(
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
     * @param scalingFactor the scaling factor used, which shifts the bids further out from the mid-price
     * @param bidPoolSize the size of the bid pool
     * @param askPoolSize the size of the ask pool
     * @param quoteRange depth of quoted prices
     * @param orderCount the target order count
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public List<DistributionStep> getBidDistribution(
            double scalingFactor,
            double bidPoolSize,
            double askPoolSize,
            double quoteRange,
            int orderCount
    ) {
        double price = bidPoolSize / askPoolSize;
        double cutoff = price * (1 - quoteRange);
        double askSize = 1 / price;
        List<DistributionStep> distribution = new ArrayList<>();
        while(price >= cutoff) {
            double bidSize = getBidSize(askSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize -= bidSize;
            askPoolSize += askSize;
            price = bidPoolSize / askPoolSize;
            askSize = askSize * (1 + STEP_SIZE);
        }
        if(distribution.size() > orderCount) {
            distribution = aggregateDistribution(distribution, orderCount);
        }
        return distribution;
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
    public List<DistributionStep> getAskDistribution(
            double scalingFactor,
            double bidPoolSize,
            double askPoolSize,
            double askQuoteRange,
            int orderCount
    ) {
        double bidSize = 1.0;
        double price = bidPoolSize / askPoolSize;
        double cutoff = price * (1 + askQuoteRange);
        List<DistributionStep> distribution = new ArrayList<>();
        while(price <= cutoff) {
            double askSize = getAskSize(bidSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize += bidSize;
            askPoolSize -= askSize;
            price = bidPoolSize / askPoolSize;
            bidSize = bidSize * (1 + STEP_SIZE);
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
    private List<DistributionStep> aggregateDistribution(
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
     * Get the scaling factor, used to push orders out from the mid-price
     *
     * @param openVolumeRatio the % of total collateral allocated to our open volume
     *
     * @return the scaling factor (a number between 0 and 1)
     */
    public double getScalingFactor(
            final double openVolumeRatio
    ) {
        return 1 - Math.abs(openVolumeRatio);
    }
}