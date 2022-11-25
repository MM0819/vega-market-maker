package com.vega.protocol.utils;

import org.apache.commons.math3.special.Erf;
import org.springframework.stereotype.Component;
import vega.Vega;

@Component
public class QuantUtils {

    /**
     * Return the probability of trading for a given price level using log-normal distribution
     *
     * @param mu from risk model
     * @param sigma from risk model
     * @param bestPrice the best price in the book
     * @param tau from risk model
     * @param lowerBound the lower price monitoring bound
     * @param upperBound the upper price monitoring bound
     * @param price the price to calculate the probability of trading for
     * @param side the side of the book {@link vega.Vega.Side}
     *
     * @return the probability of this price trading
     */
    public double getProbabilityOfTrading(
            final double mu,
            final double sigma,
            final double bestPrice,
            final double tau,
            final double lowerBound,
            final double upperBound,
            final double price,
            final Vega.Side side
    ) {
        double stdev = sigma * Math.sqrt(tau);
        double m = Math.log(bestPrice) + (mu - 0.5 * sigma * sigma) * tau;
        if(price < lowerBound || price > upperBound) {
            return 0;
        }
        double min = cdf(m, stdev, lowerBound);
        double max = cdf(m, stdev, upperBound);
        double z = max - min;
        if(side.equals(Vega.Side.SIDE_BUY)) {
            return (cdf(m, stdev, price) - min) / z;
        }
        return (max - cdf(m, stdev, price)) / z;
    }

    /**
     * Cumulative density function at x
     *
     * @param m calculated using mu, sigma and tau: Math.log(bestPrice) + (mu - 0.5 * sigma * sigma) * tau
     * @param stdev calculated using sigma and tau: sigma * Math.sqrt(tau)
     * @param x x for cdf
     *
     * @return the cdf at x
     */
    private double cdf(double m, double stdev, double x) {
        return 0.5 * Erf.erfc(-(Math.log(x) - m) / (Math.sqrt(2.0) * stdev));
    }
}