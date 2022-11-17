package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.constant.TimeInForce;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
public class NaiveFlowTask extends TradingTask {
    private final VegaApiClient vegaApiClient;
    private final MarketService marketService;
    private final OrderService orderService;
    private final String marketId;
    private final String partyId;
    private MarketSide bias = MarketSide.BUY;
    private LocalDateTime nextBiasUpdate = LocalDateTime.now().minusHours(1);

    public NaiveFlowTask(@Value("${vega.market.id}") String marketId,
                         @Value("${naive.flow.enabled}") Boolean taskEnabled,
                         @Value("${naive.flow.party.id}") String partyId,
                         VegaApiClient vegaApiClient,
                         MarketService marketService,
                         OrderService orderService,
                         ReferencePriceStore referencePriceStore,
                         DataInitializer dataInitializer,
                         WebSocketInitializer webSocketInitializer) {
        super(dataInitializer, webSocketInitializer, referencePriceStore, taskEnabled);
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.marketId = marketId;
        this.partyId = partyId;
        this.orderService = orderService;
    }

    @Override
    public String getCronExpression() {
        return "*/5 * * * * *";
    }

    @Override
    public void execute() {
        if(!isInitialized()) {
            log.warn("Cannot execute {} because data is not initialized", getClass().getSimpleName());
            return;
        }
        if(!taskEnabled) {
            log.debug("Cannot execute {} because it is disabled", getClass().getSimpleName());
            return;
        }
        updateBias();
        double threshold = new Random().nextDouble();
        MarketSide side = bias;
        if(threshold > 0.7) {
            side = orderService.getOtherSide(bias);
        }
        Market market = marketService.getById(marketId);
        BigDecimal size = BigDecimal.valueOf(1 / Math.pow(10, market.getPositionDecimalPlaces()));
        Order order = new Order()
                .setType(OrderType.MARKET)
                .setStatus(OrderStatus.ACTIVE)
                .setSide(side)
                .setSize(size)
                .setTimeInForce(TimeInForce.IOC)
                .setMarket(market)
                .setPartyId(partyId);
        vegaApiClient.submitOrder(order, partyId);
    }

    private void updateBias() {
        if(LocalDateTime.now().isAfter(nextBiasUpdate)) {
            bias = orderService.getOtherSide(bias);
            nextBiasUpdate = LocalDateTime.now().plusHours((int) (30 + (240 - 30) * new Random().nextDouble()));
        }
    }
}