package com.vega.protocol.utils;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.DoubleStream;

@Component
public class PricingUtils {

    private final AppConfigStore configStore;

    public PricingUtils(AppConfigStore configStore) {
        this.configStore = configStore;
    }

    /**
     * Build the distribution for a given mid-price, target volume and range
     *
     * @param midPrice this will be the best bid or ask
     * @param totalVolume the total volume across all quotes
     * @param range the depth for the quotes
     *
     * @return {@link List<DistributionStep>}
     */
    public List<DistributionStep> getDistribution(
            final double midPrice,
            final double totalVolume,
            final double range,
            final MarketSide side
    ) {
        AppConfig config = configStore.get().orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND));
        List<DistributionStep> distribution = new ArrayList<>();
        int count = config.getOrderCount();
        double iter = 6.0 / count;
        // TODO - limit the order count using the config
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