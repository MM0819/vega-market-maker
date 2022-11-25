package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.trading.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vega.Markets;
import vega.Vega;

import java.math.BigDecimal;

@Slf4j
@Service
public class MarketService {

    private final VegaStore vegaStore;
    private final DecimalUtils decimalUtils;
    private final ReferencePriceStore referencePriceStore;

    public MarketService(
            final VegaStore vegaStore,
            final DecimalUtils decimalUtils,
            final ReferencePriceStore referencePriceStore
    ) {
        this.vegaStore = vegaStore;
        this.decimalUtils = decimalUtils;
        this.referencePriceStore = referencePriceStore;
    }

    /**
     * Get market by ID
     *
     * @return {@link Markets.Market}
     */
    public Markets.Market getById(
            final String marketId
    ) {
        return vegaStore.getMarketById(marketId)
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_NOT_FOUND));
    }

    /**
     * Get market data by market ID
     *
     * @param marketId the market ID
     *
     * @return {@link vega.Vega.MarketData}
     */
    public Vega.MarketData getDataById(
            final String marketId
    ) {
        return vegaStore.getMarketDataById(marketId)
                .orElseThrow(() -> new TradingException(ErrorCode.MARKET_DATA_NOT_FOUND));
    }

    /**
     * Get the best price for the given market and side (using reference mid-price as a back-up)
     *
     * @param side {@link Vega.Side}
     * @param market {@link vega.Markets.Market}
     *
     * @return the best price
     */
    public double getBestPrice(
            final Vega.Side side,
            final Markets.Market market
    ) {
        var marketData = getDataById(market.getId());
        long dp = market.getDecimalPlaces();
        String bestPriceStr = side.equals(Vega.Side.SIDE_BUY) ?
                marketData.getBestBidPrice() : marketData.getBestOfferPrice();
        double bestPrice = decimalUtils.convertToDecimals(dp, new BigDecimal(bestPriceStr));
        if(bestPrice == 0) {
            ReferencePrice referencePrice = referencePriceStore.get()
                    .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
            bestPrice = referencePrice.getMidPrice();
        }
        return bestPrice;
    }
}