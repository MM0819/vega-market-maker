package com.vega.protocol.task;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vega.Markets;
import vega.Vega;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class NaiveFlowTask extends TradingTask {
    private final VegaGrpcClient vegaGrpcClient;
    private final MarketService marketService;
    private final OrderService orderService;
    private final SleepUtils sleepUtils;
    private final String partyId;
    private final DecimalUtils decimalUtils;
    private Vega.Side bias = Vega.Side.SIDE_BUY;
    private LocalDateTime nextBiasUpdate = LocalDateTime.now().minusHours(1);

    public NaiveFlowTask(@Value("${naive.flow.party.id}") String partyId,
                         VegaGrpcClient vegaGrpcClient,
                         MarketService marketService,
                         OrderService orderService,
                         ReferencePriceStore referencePriceStore,
                         DataInitializer dataInitializer,
                         WebSocketInitializer webSocketInitializer,
                         SleepUtils sleepUtils,
                         DecimalUtils decimalUtils) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.vegaGrpcClient = vegaGrpcClient;
        this.marketService = marketService;
        this.partyId = partyId;
        this.orderService = orderService;
        this.sleepUtils = sleepUtils;
        this.decimalUtils = decimalUtils;
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
        Vega.Side side = bias;
        if(threshold > 0.7) {
            side = orderService.getOtherSide(bias);
        }
        Markets.Market market = marketService.getById(marketConfig.getMarketId());
        if(!market.getState().equals(Markets.Market.State.STATE_ACTIVE)) {
            log.warn("Cannot trade; market state = {}", market.getState());
            return;
        }
        double modifier = ThreadLocalRandom.current().nextDouble(1, 10);
        double size = (1 / Math.pow(10, market.getPositionDecimalPlaces())) * modifier;
        long vegaSize = decimalUtils.convertFromDecimals(market.getPositionDecimalPlaces(), size).longValue();
        sleepUtils.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
        vegaGrpcClient.submitOrder(null, vegaSize, side,
                Vega.Order.TimeInForce.TIME_IN_FORCE_IOC, Vega.Order.Type.TYPE_MARKET, market.getId(), partyId);
    }

    private void updateBias() {
        if(LocalDateTime.now().isAfter(nextBiasUpdate)) {
            bias = orderService.getOtherSide(bias);
            nextBiasUpdate = LocalDateTime.now().plusHours((int) (30 + (240 - 30) * new Random().nextDouble()));
        }
    }
}