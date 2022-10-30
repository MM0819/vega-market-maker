package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.Position;
import com.vega.protocol.service.PositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class HedgeExposureTask extends TradingTask {

    private final PositionService positionService;
    private final IGApiClient igApiClient;
    private final BinanceApiClient binanceApiClient;
    private final String marketId;
    private final String partyId;
    private final ReferencePriceSource referencePriceSource;
    private final String referencePriceMarket;
    private final String igMarketEpic;

    public HedgeExposureTask(DataInitializer dataInitializer,
                             WebSocketInitializer webSocketInitializer,
                             @Value("${vega.market.id}") String marketId,
                             @Value("${hedge.exposure.enabled}") Boolean taskEnabled,
                             @Value("${naive.flow.party.id}") String partyId,
                             @Value("${reference.price.source}") ReferencePriceSource referencePriceSource,
                             @Value("${ig.market.epic}") String igMarketEpic,
                             @Value("${reference.price.market}") String referencePriceMarket,
                             PositionService positionService,
                             IGApiClient igApiClient,
                             BinanceApiClient binanceApiClient) {
        super(dataInitializer, webSocketInitializer, taskEnabled);
        this.positionService = positionService;
        this.marketId = marketId;
        this.partyId = partyId;
        this.referencePriceSource = referencePriceSource;
        this.igApiClient = igApiClient;
        this.binanceApiClient = binanceApiClient;
        this.referencePriceMarket = referencePriceMarket;
        this.igMarketEpic = igMarketEpic;
    }

    @Override
    public String getCronExpression() {
        return "0 * * * * *";
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
        BigDecimal exposure = positionService.getExposure(marketId);
        if(exposure.doubleValue() != 0) {
            log.info("Hedging exposure...");
            if(referencePriceSource.equals(ReferencePriceSource.BINANCE)) {
                BigDecimal hedgeExposure = binanceApiClient.getPosition(referencePriceMarket)
                        .orElse(new Position().setSize(BigDecimal.ZERO)).getSize();
                BigDecimal diff = hedgeExposure.subtract(exposure).abs();
                if(hedgeExposure.abs().doubleValue() < exposure.abs().doubleValue()) {
                    MarketSide side = exposure.doubleValue() < 0 ? MarketSide.BUY : MarketSide.SELL;
                    binanceApiClient.submitMarketOrder(referencePriceMarket, diff, side); // TODO - should TWAP this
                } else if(hedgeExposure.abs().doubleValue() > exposure.abs().doubleValue()) {
                    MarketSide side = exposure.doubleValue() < 0 ? MarketSide.SELL : MarketSide.BUY;
                    binanceApiClient.submitMarketOrder(referencePriceMarket, diff, side); // TODO - should TWAP this
                }
            } else {
                BigDecimal hedgeExposure = binanceApiClient.getPosition(igMarketEpic)
                        .orElse(new Position().setSize(BigDecimal.ZERO)).getSize();
                BigDecimal diff = hedgeExposure.subtract(exposure).abs();
                if(hedgeExposure.abs().doubleValue() < exposure.abs().doubleValue()) {
                    MarketSide side = exposure.doubleValue() < 0 ? MarketSide.BUY : MarketSide.SELL;
                    igApiClient.submitMarketOrder(igMarketEpic, diff, side); // TODO - should TWAP this
                } else if(hedgeExposure.abs().doubleValue() > exposure.abs().doubleValue()) {
                    MarketSide side = exposure.doubleValue() < 0 ? MarketSide.SELL : MarketSide.BUY;
                    igApiClient.submitMarketOrder(igMarketEpic, diff, side); // TODO - should TWAP this
                }
            }
        }
    }
}