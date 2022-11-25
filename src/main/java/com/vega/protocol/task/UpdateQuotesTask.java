package com.vega.protocol.task;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.*;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
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
@Component
public class UpdateQuotesTask extends TradingTask {

    private static final String TAU_SCALING_PARAM = "market.liquidity.probabilityOfTrading.tau.scaling";
    private static final String MAX_BATCH_SIZE_PARAM = "spam.protection.max.batchSize";
    private static final String STAKE_TO_SISKAS_PARAM = "market.liquidity.stakeToCcySiskas";
    private static final String MIN_PROB_OF_TRADING_PARAM = "market.liquidity.minimum.probabilityOfTrading.lpOrders";
    private static final String MAKER_FEE_PARAM = "market.fee.factors.makerFee";
    private final NetworkParameterService networkParameterService;
    private final VegaGrpcClient vegaGrpcClient;
    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final OrderService orderService;
    private final LiquidityProvisionService liquidityProvisionService;
    private final AssetService assetService;
    private final PricingUtils pricingUtils;
    private final QuantUtils quantUtils;
    private final DecimalUtils decimalUtils;
    private final TradingConfigRepository tradingConfigRepository;

    public UpdateQuotesTask(ReferencePriceStore referencePriceStore,
                            NetworkParameterService networkParameterService,
                            VegaGrpcClient vegaGrpcClient,
                            MarketService marketService,
                            AccountService accountService,
                            PositionService positionService,
                            OrderService orderService,
                            AssetService assetService,
                            PricingUtils pricingUtils,
                            QuantUtils quantUtils,
                            DataInitializer dataInitializer,
                            WebSocketInitializer webSocketInitializer,
                            LiquidityProvisionService liquidityProvisionService,
                            DecimalUtils decimalUtils,
                            TradingConfigRepository tradingConfigRepository) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.networkParameterService = networkParameterService;
        this.vegaGrpcClient = vegaGrpcClient;
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.orderService = orderService;
        this.assetService = assetService;
        this.pricingUtils = pricingUtils;
        this.quantUtils = quantUtils;
        this.liquidityProvisionService = liquidityProvisionService;
        this.decimalUtils = decimalUtils;
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
        List<Commands.OrderSubmission> submissions = getSubmissions(market, marketConfig, balance, partyId);
        List<Commands.OrderCancellation> cancellations = getCancellations(market);
        updateQuotes(cancellations, submissions, partyId);
    }

    /**
     * Update quotes using batch instruction
     *
     * @param cancellations order IDs to cancel
     * @param submissions new order submissions
     * @param partyId the party ID
     */
    private void updateQuotes(
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

    private double getTargetVolume(
            final MarketSide side,
            final double balance,
            final double midPrice,
            final TradingConfig tradingConfig
    ) {
        double exposure = positionService.getExposure(tradingConfig.getMarketConfig().getMarketId(),
                tradingConfig.getMarketConfig().getPartyId());
        log.info("Exposure = {}\nBalance = {}", exposure, balance);
        double askPoolSize = balance * 0.5 / midPrice;
        double volume = askPoolSize * tradingConfig.getCommitmentBalanceRatio();
        double openVolumeRatio = Math.min(0.99, Math.abs(exposure) / askPoolSize);
        if((exposure > 0 && side.equals(MarketSide.BUY)) || (exposure < 0 && side.equals(MarketSide.SELL))) {
            volume = volume * (1 - openVolumeRatio);
        }
        return volume;
    }

    /**
     * Get order submissions
     *
     * @param market {@link Markets.Market}
     * @param marketConfig {@link MarketConfig}
     * @param balance the user's balance
     * @param partyId the party ID
     * @return {@link List<Commands.OrderSubmission>}
     */
    private List<Commands.OrderSubmission> getSubmissions(
            final Markets.Market market,
            final MarketConfig marketConfig,
            final double balance,
            final String partyId
    ) {
        // TODO - we should use the net exposure here after considering our hedge on Binance / IG
        TradingConfig tradingConfig = tradingConfigRepository.findByMarketConfig(marketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        double midPrice = referencePrice.getMidPrice();
        log.info("Reference price = {}", referencePrice);
        double bidVolume = getTargetVolume(MarketSide.BUY, balance, midPrice, tradingConfig);
        double askVolume = getTargetVolume(MarketSide.SELL, balance, midPrice, tradingConfig);
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
        double spreadDelta = getSpreadDelta(bestBidPrice, bestAskPrice, market, tradingConfig.getMarketConfig());
        bidDistribution.forEach(b -> b.setPrice(b.getPrice() - spreadDelta));
        askDistribution.forEach(a -> a.setPrice(a.getPrice() + spreadDelta));
        List<Commands.OrderSubmission> bids = distributionToOrders(bidDistribution, Vega.Side.SIDE_BUY,
                tradingConfig.getBidSizeFactor(), market);
        List<Commands.OrderSubmission> asks = distributionToOrders(askDistribution, Vega.Side.SIDE_SELL,
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
    private List<Commands.OrderCancellation> getCancellations(
            final Markets.Market market
    ) {
        return orderService.getByMarketIdAndStatus(market.getId(), Vega.Order.Status.STATUS_ACTIVE).stream()
                .filter(o -> StringUtils.isEmpty(o.getLiquidityProvisionId()))
                .map(o -> Commands.OrderCancellation.newBuilder()
                        .setMarketId(market.getId())
                        .setOrderId(o.getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convert {@link DistributionStep}s to {@link Commands.OrderSubmission}s
     *
     * @param distribution {@link List<DistributionStep>}
     * @param side {@link Vega.Side}
     * @param sizeScalingFactor scaling factor for order sizes
     * @param market {@link vega.Markets.Market}
     *
     * @return {@link List<Commands.OrderSubmission>}
     */
    private List<Commands.OrderSubmission> distributionToOrders(
            final List<DistributionStep> distribution,
            final Vega.Side side,
            final Double sizeScalingFactor,
            final Markets.Market market
    ) {
        return distribution.stream().map(d -> {
            double size = d.getSize() * sizeScalingFactor;
            return Commands.OrderSubmission.newBuilder()
                    .setSize(decimalUtils.convertFromDecimals(market.getPositionDecimalPlaces(), size).longValue())
                    .setPrice(decimalUtils.convertFromDecimals(market.getDecimalPlaces(), d.getPrice())
                            .toBigInteger().toString())
                    .setSide(side)
                    .setType(Vega.Order.Type.TYPE_LIMIT)
                    .setTimeInForce(Vega.Order.TimeInForce.TIME_IN_FORCE_GTC)
                    .setMarketId(market.getId())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get the target spread based on efficiency of hedging exposure
     *
     * @param hedgeSpread the spread on the hedging market
     * @param targetEdge the edge that we want to target
     * @param hedgeFee the fee on the hedging market
     * @param liquidityRebate the liquidity rebate on Vega
     * @param makerRebate the maker rebate on Vega
     *
     * @return the spread for Vega quotes
     */
    private double getTargetSpread(
            final double hedgeSpread,
            final double targetEdge,
            final double hedgeFee,
            final double liquidityRebate,
            final double makerRebate
    ) {
        return targetEdge + hedgeFee + liquidityRebate + makerRebate + hedgeSpread;
    }

    /**
     * Applies the target spread to bids and asks
     *
     * @param bestBidPrice the best bid price
     * @param bestAskPrice the best ask price
     * @param market {@link Markets.Market}
     * @param marketConfig {@link MarketConfig}
     */
    private double getSpreadDelta(
            final double bestBidPrice,
            final double bestAskPrice,
            final Markets.Market market,
            final MarketConfig marketConfig
    ) {
        double makerFee = networkParameterService.getAsInt(MAKER_FEE_PARAM);
        double hedgeSpread = (bestAskPrice - bestBidPrice) / bestBidPrice;
        double liquidityFee = Double.parseDouble(market.getFees().getFactors().getLiquidityFee());
        double liquidityRebate = Math.min(liquidityFee, liquidityFee * -1);
        double makerRebate = Math.min(makerFee, makerFee * -1);
        double targetSpread = getTargetSpread(hedgeSpread, marketConfig.getTargetEdge(),
                marketConfig.getHedgeFee(), liquidityRebate, makerRebate);
        double currentSpread = (bestAskPrice - bestBidPrice) / bestBidPrice;
        double delta = 0;
        if(currentSpread < targetSpread) {
            delta = (targetSpread - currentSpread) * bestBidPrice * 0.5;
        }
        return delta;
    }

    /**
     * Get the best price for the given market and side (using reference mid-price as a back-up)
     *
     * @param side {@link Vega.Side}
     * @param market {@link vega.Markets.Market}
     *
     * @return the best price
     */
    private double getBestPrice(
            final Vega.Side side,
            final Markets.Market market
    ) {
        var marketData = marketService.getDataById(market.getId());
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
            double bestPrice = getBestPrice(order.getSide(), market);
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
     * Scale order sizes when a liquidity commitment exists
     *
     * @param market        {@link vega.Markets.Market}
     * @param partyId       the party ID
     * @param bids          {@link List<Commands.OrderSubmission>}
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
}
