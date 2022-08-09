package com.vega.protocol.utils;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@Component
public class PricingUtils {

    private final AppConfigStore appConfigStore;

    public PricingUtils(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

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
     * @param quoteRange the range to provide quotes for
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
        double stepSize = appConfigStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND))
                .getPricingStepSize();
        stepSize = askSize * stepSize;
        while(price >= cutoff) {
            double bidSize = getBidSize(askSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize -= bidSize;
            askPoolSize += askSize;
            price = bidPoolSize / askPoolSize;
            askSize = askSize + stepSize;
        }
        if(distribution.size() > orderCount) {
            distribution = aggregateDistribution(distribution, orderCount, MarketSide.BUY);
        }
        Collections.reverse(distribution);
        return distribution;
    }

    /**
     * Get the ask distribution
     *
     * @param scalingFactor scaling factor, used to push asks out from mid-price
     * @param bidPoolSize the size of our bid pool (i.e. total collateral)
     * @param askPoolSize the size of our ask pool (derived from bid pool and reference price)
     * @param quoteRange the range to provide quotes for
     * @param orderCount the number of orders
     *
     * @return {@link List} of {@link DistributionStep}
     */
    public List<DistributionStep> getAskDistribution(
            double scalingFactor,
            double bidPoolSize,
            double askPoolSize,
            double quoteRange,
            int orderCount
    ) {
        double bidSize = 1.0;
        double price = bidPoolSize / askPoolSize;
        double cutoff = price * (1 + quoteRange);
        List<DistributionStep> distribution = new ArrayList<>();
        double stepSize = appConfigStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND))
                .getPricingStepSize();
        stepSize = bidSize * stepSize;
        while(price <= cutoff) {
            double askSize = getAskSize(bidSize, askPoolSize, bidPoolSize, scalingFactor);
            DistributionStep order = new DistributionStep().setPrice(price).setSize(askSize);
            distribution.add(order);
            bidPoolSize += bidSize;
            askPoolSize -= askSize;
            price = bidPoolSize / askPoolSize;
            bidSize = bidSize + stepSize;
        }
        if(distribution.size() > orderCount) {
            distribution = aggregateDistribution(distribution, orderCount, MarketSide.SELL);
        }
        return distribution;
    }

    /**
     * Aggregate the orders in the distribution
     *
     * @param distribution {@link List<DistributionStep>}
     * @param orderCount the number of orders
     * @param side {@link MarketSide}
     *
     * @return {@link List<DistributionStep>}
     */
    private List<DistributionStep> aggregateDistribution(
            List<DistributionStep> distribution,
            int orderCount,
            MarketSide side
    ) {
        double maxPrice = distribution.stream().max(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        double minPrice = distribution.stream().min(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        double range = maxPrice - minPrice;
        double stepSize = range / orderCount;
        List<Double> aggregatePrices = DoubleStream.iterate(minPrice, d -> d + stepSize)
                .boxed()
                .limit((int) (1 + (maxPrice - minPrice) / stepSize))
                .toList();
        double previousPrice = 0;
        List<DistributionStep> aggregateDistribution = new ArrayList<>();
        for(double price : aggregatePrices) {
            double finalPreviousPrice = previousPrice;
            if(side.equals(MarketSide.BUY)) {
                double notionalSize = distribution.stream()
                        .filter(d -> d.getPrice() > finalPreviousPrice && d.getPrice() <= price)
                        .map(d -> d.getPrice() * d.getSize())
                        .mapToDouble(d -> d)
                        .sum();
                aggregateDistribution.add(new DistributionStep()
                        .setPrice(price)
                        .setSize(notionalSize / price));
            } else {
                double size = distribution.stream()
                        .filter(d -> d.getPrice() > finalPreviousPrice && d.getPrice() <= price)
                        .mapToDouble(DistributionStep::getSize)
                        .sum();
                aggregateDistribution.add(new DistributionStep()
                        .setPrice(previousPrice)
                        .setSize(size));
            }
            previousPrice = price;
        }
        aggregateDistribution.remove(0);
        return aggregateDistribution;
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
        return Math.max(0, 1 - Math.abs(openVolumeRatio));
    }
}