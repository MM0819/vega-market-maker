package com.vega.protocol.service;

import com.vega.protocol.api.ExchangeApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.model.trading.DistributionStep;
import com.vega.protocol.model.trading.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import vega.Assets;
import vega.Markets;
import vega.Vega;
import vega.commands.v1.Commands;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private static final String STAKE_TO_SISKAS_PARAM = "market.liquidity.stakeToCcySiskas";
    private static final String TAU_SCALING_PARAM = "market.liquidity.probabilityOfTrading.tau.scaling";
    private static final String MIN_PROB_OF_TRADING_PARAM = "market.liquidity.minimum.probabilityOfTrading.lpOrders";
    private static final String MAKER_FEE_PARAM = "market.fee.factors.makerFee";
    private static final String MAX_BATCH_SIZE_PARAM = "spam.protection.max.batchSize";

    private final VegaStore vegaStore;
    private final ReferencePriceStore referencePriceStore;
    private final LiquidityProvisionService liquidityProvisionService;
    private final AssetService assetService;
    private final MarketService marketService;
    private final NetworkParameterService networkParameterService;
    private final PositionService positionService;
    private final DecimalUtils decimalUtils;
    private final QuantUtils quantUtils;
    private final PricingUtils pricingUtils;
    private final SleepUtils sleepUtils;
    private final VegaGrpcClient vegaGrpcClient;

    public OrderService(
            final VegaStore vegaStore,
            final ReferencePriceStore referencePriceStore,
            final LiquidityProvisionService liquidityProvisionService,
            final AssetService assetService,
            final MarketService marketService,
            final NetworkParameterService networkParameterService,
            final PositionService positionService,
            final DecimalUtils decimalUtils,
            final QuantUtils quantUtils,
            final PricingUtils pricingUtils,
            final SleepUtils sleepUtils,
            final VegaGrpcClient vegaGrpcClient) {
        this.vegaStore = vegaStore;
        this.referencePriceStore = referencePriceStore;
        this.liquidityProvisionService = liquidityProvisionService;
        this.assetService = assetService;
        this.marketService = marketService;
        this.networkParameterService = networkParameterService;
        this.positionService = positionService;
        this.decimalUtils = decimalUtils;
        this.quantUtils = quantUtils;
        this.pricingUtils = pricingUtils;
        this.sleepUtils = sleepUtils;
        this.vegaGrpcClient = vegaGrpcClient;
    }

    public List<Vega.Order> getByMarketIdAndStatus(
            final String marketId,
            final Vega.Order.Status status
    ) {
        return vegaStore.getOrdersByMarketIdAndStatus(marketId, status);
    }

    /**
     * Get the opposite {@link MarketSide}
     *
     * @param side {@link MarketSide}
     *
     * @return opposite {@link MarketSide}
     */
    public Vega.Side getOtherSide(
            Vega.Side side
    ) {
        return side.equals(Vega.Side.SIDE_BUY) ? Vega.Side.SIDE_SELL : Vega.Side.SIDE_BUY;
    }

    /**
     * Scale order sizes when a liquidity commitment exists
     *
     * @param market        {@link vega.Markets.Market}
     * @param partyId       the party ID
     * @param bids          {@link List< Commands.OrderSubmission>}
     * @param asks          {@link List<Commands.OrderSubmission>}
     * @param tradingConfig {@link TradingConfig}
     */
    private void scaleOrdersForLiquidityCommitment(
            final Markets.Market market,
            final String partyId,
            final List<Commands.OrderSubmission> bids,
            final List<Commands.OrderSubmission> asks,
            final TradingConfig tradingConfig
    ) {
        var lp = liquidityProvisionService.getByMarketIdAndPartyId(market.getId(), partyId);
        if(lp.isPresent()) {
            Assets.Asset asset = assetService.getById(market.getTradableInstrument()
                    .getInstrument().getFuture().getSettlementAsset());
            double commitmentAmount = decimalUtils.convertToDecimals(asset.getDetails().getDecimals(),
                    new BigDecimal(lp.get().getCommitmentAmount()));
            scaleOrders(bids, commitmentAmount, tradingConfig);
            scaleOrders(asks, commitmentAmount, tradingConfig);
        }
    }

    /**
     * Scale up order sizes so that the quotes satisfy the LP commitment amount and pegs are not auto-deployed
     *
     * @param orders {@link List<Commands.OrderSubmission>}
     * @param commitmentAmount the LP commitment amount
     * @param tradingConfig {@link TradingConfig}
     */
    private void scaleOrders(
            final List<Commands.OrderSubmission> orders,
            final double commitmentAmount,
            final TradingConfig tradingConfig
    ) {
        double stakeToSiskas = networkParameterService.getAsDouble(STAKE_TO_SISKAS_PARAM);
        double effectiveVolume = getEffectiveVolume(orders);
        double targetVolume = (commitmentAmount * (1 + tradingConfig.getStakeBuffer())) * stakeToSiskas;
        double volumeRatio = effectiveVolume / targetVolume;
        if(volumeRatio < 1) {
            double modifier = 1.0 / volumeRatio;
            orders.forEach(o -> Commands.OrderSubmission.newBuilder(o)
                    .setSize(Math.round(o.getSize() * modifier)).build());
        }
    }

    /**
     * Calculate the effective volume implied by orders after considering probability of a price-level trading
     *
     * @param orders {@link List<Commands.OrderSubmission>}
     *
     * @return effective total volume
     */
    private double getEffectiveVolume(
            final List<Commands.OrderSubmission> orders
    ) {
        double tauScaling = networkParameterService.getAsDouble(TAU_SCALING_PARAM);
        double minProbabilityOfTrading = networkParameterService.getAsDouble(MIN_PROB_OF_TRADING_PARAM);
        return orders.stream().mapToDouble(order -> {
            Markets.Market market = marketService.getById(order.getMarketId());
            Vega.MarketData marketData = marketService.getDataById(order.getMarketId());
            double minValidPrice = marketData.getPriceMonitoringBoundsList().stream()
                    .map(p -> decimalUtils.convertToDecimals(market.getDecimalPlaces(),
                            new BigDecimal(p.getMinValidPrice())))
                    .max(Double::compareTo)
                    .orElse(0.0);
            double maxValidPrice = marketData.getPriceMonitoringBoundsList().stream()
                    .map(p -> decimalUtils.convertToDecimals(market.getDecimalPlaces(),
                            new BigDecimal(p.getMaxValidPrice())))
                    .min(Double::compareTo)
                    .orElse(0.0);
            var riskModel = market.getTradableInstrument().getLogNormalRiskModel();
            double tau = riskModel.getTau() * tauScaling;
            double bestPrice = marketService.getBestPrice(order.getSide(), market);
            double orderPrice = decimalUtils.convertToDecimals(
                    market.getDecimalPlaces(), new BigDecimal(order.getPrice()));
            double orderSize = decimalUtils.convertToDecimals(
                    market.getPositionDecimalPlaces(), new BigDecimal(order.getSize()));
            double probability = quantUtils.getProbabilityOfTrading(riskModel.getParams().getMu(),
                    riskModel.getParams().getSigma(), bestPrice, tau, minValidPrice,
                    maxValidPrice, orderPrice, order.getSide());
            return orderSize * orderPrice * Math.max(minProbabilityOfTrading, probability);
        }).sum();
    }


    /**
     * Get order submissions
     *
     * @param market {@link Markets.Market}
     * @param tradingConfig {@link TradingConfig}
     * @param balance the user's balance
     * @param partyId the party ID
     * @return {@link List<Commands.OrderSubmission>}
     */
    public List<Commands.OrderSubmission> getSubmissions(
            final Markets.Market market,
            final TradingConfig tradingConfig,
            final double balance,
            final String partyId
    ) {
        // TODO - we should use the net exposure here after considering our hedge on Binance / IG
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        double midPrice = referencePrice.getMidPrice();
        log.info("Reference price = {}", referencePrice);
        double exposure = positionService.getExposure(tradingConfig.getMarketConfig().getMarketId(),
                tradingConfig.getMarketConfig().getPartyId());
        log.info("Exposure = {}\nBalance = {}", exposure, balance);
        double bidVolume = pricingUtils.getTargetVolume(MarketSide.BUY, balance, exposure, midPrice, tradingConfig);
        double askVolume = pricingUtils.getTargetVolume(MarketSide.SELL, balance, exposure, midPrice, tradingConfig);
        List<DistributionStep> askDistribution = pricingUtils.getDistribution(
                referencePrice.getAskPrice(), askVolume, tradingConfig.getAskQuoteRange(),
                MarketSide.SELL, tradingConfig.getQuoteOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getDistribution(
                referencePrice.getBidPrice(), bidVolume, tradingConfig.getBidQuoteRange(),
                MarketSide.BUY, tradingConfig.getQuoteOrderCount());
        if(bidDistribution.size() == 0) {
            log.warn("Bid distribution was empty !!");
            throw new TradingException(ErrorCode.EMPTY_DISTRIBUTION);
        }
        if(askDistribution.size() == 0) {
            log.warn("Ask distribution was empty !!");
            throw new TradingException(ErrorCode.EMPTY_DISTRIBUTION);
        }
        askDistribution.sort(Comparator.comparing(DistributionStep::getPrice));
        bidDistribution.sort(Comparator.comparing(DistributionStep::getPrice).reversed());
        double bestBidPrice = bidDistribution.get(0).getPrice();
        double bestAskPrice = askDistribution.get(0).getPrice();
        double makerFee = networkParameterService.getAsDouble(MAKER_FEE_PARAM);
        double spreadDelta = pricingUtils.getSpreadDelta(bestBidPrice, bestAskPrice, makerFee,
                market, tradingConfig.getMarketConfig());
        bidDistribution.forEach(b -> b.setPrice(b.getPrice() - spreadDelta));
        askDistribution.forEach(a -> a.setPrice(a.getPrice() + spreadDelta));
        List<Commands.OrderSubmission> bids = pricingUtils.distributionToOrders(bidDistribution, Vega.Side.SIDE_BUY,
                tradingConfig.getBidSizeFactor(), market);
        List<Commands.OrderSubmission> asks = pricingUtils.distributionToOrders(askDistribution, Vega.Side.SIDE_SELL,
                tradingConfig.getAskSizeFactor(), market);
        // TODO - there might be times when we want to offer an arb to the market in order to reduce our exposure, if
        //  we want to do that, we would simply shift the mid-price here and that will attract price-takers
        scaleOrdersForLiquidityCommitment(market, partyId, bids, asks, tradingConfig);
        List<Commands.OrderSubmission> submissions = new ArrayList<>();
        submissions.addAll(bids);
        submissions.addAll(asks);
        return submissions;
    }

    /**
     * Get order IDs to cancel
     *
     * @param market {@link vega.Markets.Market}
     *
     * @return {@link List<Commands.OrderCancellation>}
     */
    public List<Commands.OrderCancellation> getCancellations(
            final Markets.Market market
    ) {
        return getByMarketIdAndStatus(market.getId(), Vega.Order.Status.STATUS_ACTIVE).stream()
                .filter(o -> StringUtils.isEmpty(o.getLiquidityProvisionId()))
                .map(o -> Commands.OrderCancellation.newBuilder()
                        .setMarketId(market.getId())
                        .setOrderId(o.getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Update quotes using batch instruction
     *
     * @param cancellations order IDs to cancel
     * @param submissions new order submissions
     * @param partyId the party ID
     */
    public void updateQuotes(
            final List<Commands.OrderCancellation> cancellations,
            final List<Commands.OrderSubmission> submissions,
            final String partyId
    ) {
        int maxBatchSize = networkParameterService.getAsInt(MAX_BATCH_SIZE_PARAM);
        int totalBatchSize = cancellations.size() + submissions.size();
        log.info("Max batch size = {}; Total batch size = {}; Cancellations = {}; Submissions = {}",
                maxBatchSize, totalBatchSize, cancellations.size(), submissions.size());
        if (totalBatchSize <= maxBatchSize && totalBatchSize > 0) {
            vegaGrpcClient.batchMarketInstruction(Collections.emptyList(), cancellations, submissions, partyId);
        } else {
            List<List<Commands.OrderCancellation>> cancellationBatches = ListUtils.partition(cancellations, maxBatchSize);
            List<List<Commands.OrderSubmission>> submissionBatches = ListUtils.partition(submissions, maxBatchSize);
            for (List<Commands.OrderSubmission> batch : submissionBatches) {
                vegaGrpcClient.batchMarketInstruction(Collections.emptyList(), Collections.emptyList(), batch, partyId);
            }
            for (List<Commands.OrderCancellation> batch : cancellationBatches) {
                vegaGrpcClient.batchMarketInstruction(Collections.emptyList(), batch, Collections.emptyList(), partyId);
            }
        }
        log.info("Quotes successfully updated!");
    }


    /**
     * Execute a TWAP trade
     *
     * @param side {@link MarketSide}
     * @param symbol the market symbol
     * @param totalSize the total trade size
     * @param exchangeApiClient {@link ExchangeApiClient}
     */
    public void executeTwap(
            final MarketSide side,
            final String symbol,
            final double totalSize,
            final ExchangeApiClient exchangeApiClient
    ) {
        log.info("TWAP >> {} {} {}", side, totalSize, symbol);
        double remainingSize = totalSize;
        while(remainingSize > 0) {
            ReferencePrice referencePrice = referencePriceStore.get()
                    .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
            double size = side.equals(MarketSide.BUY) ? referencePrice.getAskSize() : referencePrice.getBidSize();
            if(size > remainingSize) {
                size = remainingSize;
                remainingSize = 0.0;
            } else {
                remainingSize = remainingSize - size;
            }
            exchangeApiClient.submitMarketOrder(symbol, size, side);
            log.info("TWAP >> Remaining size = {}", remainingSize);
            sleepUtils.sleep(500L);
        }
    }
}