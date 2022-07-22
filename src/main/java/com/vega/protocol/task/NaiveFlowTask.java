package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
public class NaiveFlowTask extends TradingTask {

    private static final double DEFAULT_SIZE = 0.000001;

    private final VegaApiClient vegaApiClient;
    private final MarketService marketService;
    private final AccountService accountService;
    private final OrderService orderService;
    private final String marketId;
    private final Boolean naiveFlowEnabled;
    private final String partyId;

    private MarketSide bias = MarketSide.BUY;
    private LocalDateTime nextBiasUpdate = LocalDateTime.now().minusHours(1);

    public NaiveFlowTask(@Value("${vega.market.id}") String marketId,
                         @Value("${naive.flow.enabled}") Boolean naiveFlowEnabled,
                         @Value("${naive.flow.party.id}") String partyId,
                         VegaApiClient vegaApiClient,
                         MarketService marketService,
                         AccountService accountService,
                         OrderService orderService) {
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.marketId = marketId;
        this.naiveFlowEnabled = naiveFlowEnabled;
        this.partyId = partyId;
        this.accountService = accountService;
        this.orderService = orderService;
    }

    @Override
    public String getCronExpression() {
        return "*/5 * * * * *";
    }

    @Override
    public void execute() {
        if(!naiveFlowEnabled) return;
        updateBias();
        double threshold = new Random().nextDouble();
        MarketSide side = bias;
        if(threshold > 0.7) {
            side = orderService.getOtherSide(bias);
        }
        Market market = marketService.getById(marketId);
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        BigDecimal size = BigDecimal.valueOf(DEFAULT_SIZE)
                .multiply(BigDecimal.valueOf(new Random().nextDouble())).multiply(balance);
        Order order = new Order()
                .setType(OrderType.MARKET)
                .setStatus(OrderStatus.NEW)
                .setSide(side)
                .setSize(size)
                .setMarket(market)
                .setPartyId(partyId);
        vegaApiClient.submitOrder(order);
    }

    private void updateBias() {
        if(LocalDateTime.now().isAfter(nextBiasUpdate)) {
            bias = orderService.getOtherSide(bias);
            nextBiasUpdate = LocalDateTime.now().plusHours((int) (30 + (240 - 30) * new Random().nextDouble()));
        }
    }
}