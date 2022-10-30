package com.vega.protocol.utils;

import com.vega.protocol.constant.MarketSide;
import org.apache.commons.math3.special.Erf;
import org.springframework.stereotype.Component;

@Component
public class QuantUtils {
    public double getProbabilityOfTrading(
            final double mu,
            final double sigma,
            final double s,
            final double tau,
            final double lowerBound,
            final double upperBound,
            final double price,
            final MarketSide side
    ) {
        double stdev = sigma * Math.sqrt(tau);
        double m = Math.log(s) + (mu - 0.5 * sigma * sigma) * tau;
        double min = 0.0;
        double max = 1.0;
        double z = 1.0;
        if(lowerBound > 0.0 && upperBound > 0.0) {
            if(price < lowerBound || price > upperBound) {
                return 0;
            }
            min = cdf(m, stdev, lowerBound);
            max = cdf(m, stdev, upperBound);
            z = max - min;
        }
        if(side.equals(MarketSide.BUY)) {
            return (cdf(m, stdev, price) - min) / z;
        }
        return (max - cdf(m, stdev, price)) / z;
    }

    private double cdf(double m, double stdev, double x) {
        return 0.5 * Erf.erfc(-(Math.log(x) - m) / (Math.sqrt(2.0) * stdev));
    }
}