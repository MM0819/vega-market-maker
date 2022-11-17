package com.vega.protocol.utils;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.DistributionStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PricingUtils {

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
}