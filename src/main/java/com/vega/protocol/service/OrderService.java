package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {

    private final AppConfigStore appConfigStore;

    public OrderService(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

    /**
     * Get the new orders to quote for a given side
     *
     * @param side {@link MarketSide}
     * @param poolSize the sum of volume to quote on the given side
     * @param referencePrice the reference price of the underlying asset
     *
     * @return {@link List < Order >}
     */
    public List<Order> getMarketMakerOrders(
            final MarketSide side,
            final BigDecimal poolSize,
            final BigDecimal referencePrice
    ) {
        AppConfig config = appConfigStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND));
        List<Order> orders = new ArrayList<>();
        double quoteRange = side.equals(MarketSide.BUY) ? config.getBidQuoteRange() : config.getAskQuoteRange();
        BigDecimal stepSize = BigDecimal.valueOf(quoteRange / config.getOrderCount()).multiply(referencePrice);
        BigDecimal offset = BigDecimal.valueOf(config.getSpread()).multiply(BigDecimal.valueOf(0.5))
                .multiply(referencePrice);
        if(side.equals(MarketSide.BUY)) {
            offset = offset.multiply(BigDecimal.valueOf(-1));
        }
        for(int i=0; i<config.getOrderCount(); i++) {
            BigDecimal currentStep = stepSize.multiply(BigDecimal.valueOf(i+1));
            if(side.equals(MarketSide.BUY)) {
                currentStep = currentStep.multiply(BigDecimal.valueOf(-1));
            }
            BigDecimal price = referencePrice.add(currentStep.add(offset));
            double skew = 0.4 + (1.6 - 0.4) * new Random().nextDouble();
            BigDecimal size = poolSize.multiply(BigDecimal.valueOf(skew))
                    .divide(BigDecimal.valueOf(config.getOrderCount()), 4, RoundingMode.HALF_DOWN);
            Order order = new Order()
                    .setType(OrderType.LIMIT)
                    .setStatus(OrderStatus.NEW)
                    .setSide(side)
                    .setSize(size)
                    .setPrice(price);
            orders.add(order);
        }
        return orders;
    }

    /**
     * Get the opposite {@link MarketSide}
     *
     * @param side {@link MarketSide}
     *
     * @return opposite {@link MarketSide}
     */
    public MarketSide getOtherSide(
            MarketSide side
    ) {
        return side.equals(MarketSide.BUY) ? MarketSide.SELL : MarketSide.BUY;
    }
}