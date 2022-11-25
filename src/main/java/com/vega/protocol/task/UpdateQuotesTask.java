package com.vega.protocol.task;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vega.Markets;
import vega.commands.v1.Commands;

import java.util.List;

@Slf4j
@Component
public class UpdateQuotesTask extends TradingTask {

    private final MarketService marketService;
    private final AccountService accountService;
    private final OrderService orderService;
    private final TradingConfigRepository tradingConfigRepository;

    public UpdateQuotesTask(ReferencePriceStore referencePriceStore,
                            MarketService marketService,
                            AccountService accountService,
                            OrderService orderService,
                            DataInitializer dataInitializer,
                            WebSocketInitializer webSocketInitializer,
                            TradingConfigRepository tradingConfigRepository) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.marketService = marketService;
        this.accountService = accountService;
        this.orderService = orderService;
        this.tradingConfigRepository = tradingConfigRepository;
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
        String marketId = marketConfig.getMarketId();
        String partyId = marketConfig.getPartyId();
        log.info("Updating quotes...");
        Markets.Market market = marketService.getById(marketId);
        if(!market.getState().equals(Markets.Market.State.STATE_ACTIVE)) {
            log.warn("Cannot trade; market state = {}", market.getState());
            return;
        }
        double balance = accountService.getTotalBalance(market.getTradableInstrument()
                .getInstrument().getFuture().getSettlementAsset());
        if(balance == 0) {
            log.info("Cannot update quotes because balance = {}", balance);
            return;
        }
        TradingConfig tradingConfig = tradingConfigRepository.findByMarketConfig(marketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        List<Commands.OrderSubmission> submissions = orderService.getSubmissions(market, tradingConfig, balance, partyId);
        List<Commands.OrderCancellation> cancellations = orderService.getCancellations(market);
        orderService.updateQuotes(cancellations, submissions, partyId);
    }
}
