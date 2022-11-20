package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.constant.TimeInForce;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class NaiveFlowTask extends TradingTask {
    private final VegaApiClient vegaApiClient;
    private final MarketService marketService;
    private final OrderService orderService;
    private final SleepUtils sleepUtils;
    private final String partyId;
    private MarketSide bias = MarketSide.BUY;
    private LocalDateTime nextBiasUpdate = LocalDateTime.now().minusHours(1);

    public NaiveFlowTask(@Value("${naive.flow.party.id}") String partyId,
                         VegaApiClient vegaApiClient,
                         MarketService marketService,
                         OrderService orderService,
                         ReferencePriceStore referencePriceStore,
                         DataInitializer dataInitializer,
                         WebSocketInitializer webSocketInitializer,
                         SleepUtils sleepUtils) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.partyId = partyId;
        this.orderService = orderService;
        this.sleepUtils = sleepUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(
            final MarketConfig marketConfig
    ) {
        if(!isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        updateBias();
        double threshold = new Random().nextDouble();
        MarketSide side = bias;
        if(threshold > 0.7) {
            side = orderService.getOtherSide(bias);
        }
        Market market = marketService.getById(marketConfig.getMarketId());
        double modifier = ThreadLocalRandom.current().nextDouble(1, 10);
        BigDecimal size = BigDecimal.valueOf(1 / Math.pow(10, market.getPositionDecimalPlaces()));
        Order order = new Order()
                .setType(OrderType.MARKET)
                .setStatus(OrderStatus.ACTIVE)
                .setSide(side)
                .setSize(size.multiply(BigDecimal.valueOf(modifier)))
                .setTimeInForce(TimeInForce.IOC)
                .setMarket(market)
                .setPartyId(partyId);
        sleepUtils.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
        vegaApiClient.submitOrder(order, partyId);
    }

    private void updateBias() {
        if(LocalDateTime.now().isAfter(nextBiasUpdate)) {
            bias = orderService.getOtherSide(bias);
            nextBiasUpdate = LocalDateTime.now().plusHours((int) (30 + (240 - 30) * new Random().nextDouble()));
        }
    }
}