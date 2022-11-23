package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.*;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.NetworkParameterService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpdateQuotesTask extends TradingTask {

    private static final String TAU_SCALING_PARAM = "market.liquidity.probabilityOfTrading.tau.scaling";
    private static final String MAX_BATCH_SIZE_PARAM = "spam.protection.max.batchSize";
    private static final String STAKE_TO_SISKAS_PARAM = "market.liquidity.stakeToCcySiskas";
    private static final String MIN_PROB_OF_TRADING_PARAM = "market.liquidity.minimum.probabilityOfTrading.lpOrders";
    private static final String MAKER_FEE_PARAM = "market.fee.factors.makerFee";

    private final OrderStore orderStore;
    private final LiquidityCommitmentStore liquidityCommitmentStore;
    private final NetworkParameterService networkParameterService;
    private final VegaApiClient vegaApiClient;
    private final MarketService marketService;
    private final AccountService accountService;
    private final PositionService positionService;
    private final PricingUtils pricingUtils;
    private final QuantUtils quantUtils;
    private final TradingConfigRepository tradingConfigRepository;

    public UpdateQuotesTask(ReferencePriceStore referencePriceStore,
                            OrderStore orderStore,
                            LiquidityCommitmentStore liquidityCommitmentStore,
                            NetworkParameterService networkParameterService,
                            VegaApiClient vegaApiClient,
                            MarketService marketService,
                            AccountService accountService,
                            PositionService positionService,
                            PricingUtils pricingUtils,
                            QuantUtils quantUtils,
                            DataInitializer dataInitializer,
                            WebSocketInitializer webSocketInitializer,
                            TradingConfigRepository tradingConfigRepository) {
        super(dataInitializer, webSocketInitializer, referencePriceStore);
        this.orderStore = orderStore;
        this.liquidityCommitmentStore = liquidityCommitmentStore;
        this.networkParameterService = networkParameterService;
        this.vegaApiClient = vegaApiClient;
        this.marketService = marketService;
        this.accountService = accountService;
        this.positionService = positionService;
        this.pricingUtils = pricingUtils;
        this.quantUtils = quantUtils;
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
        String marketId = marketConfig.getMarketId();;
        String partyId = marketConfig.getPartyId();
        log.info("Updating quotes...");
        Market market = marketService.getById(marketId);
        if(!market.getState().equals(MarketState.ACTIVE)) {
            log.warn("Cannot trade; market state = {}", market.getState());
            return;
        }
        double balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance == 0) {
            log.info("Cannot update quotes because balance = {}", balance);
            return;
        }
        List<Order> submissions = getSubmissions(market, partyId, marketConfig, balance);
        List<String> cancellations = getCancellations();
        updateQuotes(cancellations, submissions, market, partyId);
    }

    /**
     * Update quotes using batch instruction
     *
     * @param cancellations order IDs to cancel
     * @param submissions new order submissions
     * @param market {@link Market}
     * @param partyId the party ID
     */
    private void updateQuotes(
            final List<String> cancellations,
            final List<Order> submissions,
            final Market market,
            final String partyId
    ) {
        int maxBatchSize = networkParameterService.getAsInt(MAX_BATCH_SIZE_PARAM);
        int totalBatchSize = cancellations.size() + submissions.size();
        log.info("Max batch size = {}; Total batch size = {}; Cancellations = {}; Submissions = {}",
                maxBatchSize, totalBatchSize, cancellations.size(), submissions.size());
        if (totalBatchSize <= maxBatchSize && totalBatchSize > 0) {
            vegaApiClient.submitBulkInstruction(cancellations, submissions, market, partyId);
        } else {
            List<List<String>> cancellationBatches = ListUtils.partition(cancellations, maxBatchSize);
            List<List<Order>> submissionBatches = ListUtils.partition(submissions, maxBatchSize);
            for (List<Order> batch : submissionBatches) {
                vegaApiClient.submitBulkInstruction(Collections.emptyList(), batch, market, partyId);
            }
            for (List<String> batch : cancellationBatches) {
                vegaApiClient.submitBulkInstruction(batch, Collections.emptyList(), market, partyId);
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
        double exposure = positionService.getExposure(tradingConfig.getMarketConfig().getMarketId());
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
     * @param market {@link Market}
     * @param partyId the party ID
     * @param marketConfig {@link MarketConfig}
     * @param balance the user's balance
     *
     * @return {@link List<Order>}
     */
    private List<Order> getSubmissions(
            final Market market,
            final String partyId,
            final MarketConfig marketConfig,
            final double balance
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
        List<Order> bids = distributionToOrders(bidDistribution, MarketSide.BUY,
                tradingConfig.getBidSizeFactor(), market, partyId);
        List<Order> asks = distributionToOrders(askDistribution, MarketSide.SELL,
                tradingConfig.getAskSizeFactor(), market, partyId);
        updateSpread(bids, asks, market, tradingConfig.getMarketConfig());
        // TODO - there might be times when we want to offer an arb to the market in order to reduce our exposure, if
        //  we want to do that, we would simply shift the mid-price here and that will attract price-takers
        scaleOrdersForLiquidityCommitment(market.getId(), bids, asks, tradingConfig);
        List<Order> submissions = new ArrayList<>();
        submissions.addAll(bids);
        submissions.addAll(asks);
        return submissions;
    }

    /**
     * Get order IDs to cancel
     *
     * @return {@link List<String>}
     */
    private List<String> getCancellations() {
        List<Order> currentOrders = orderStore.getItems().stream().filter(o -> !o.isPeggedOrder())
                .filter(o -> o.getStatus().equals(OrderStatus.ACTIVE)).toList();
        return currentOrders.stream().map(Order::getId).toList();
    }

    /**
     * Convert {@link DistributionStep}s to {@link Order}s
     *
     * @param distribution {@link List<DistributionStep>}
     * @param side {@link MarketSide}
     * @param sizeScalingFactor scaling factor for order sizes
     * @param market {@link Market}
     * @param partyId the party ID
     *
     * @return {@link List<Order>}
     */
    private List<Order> distributionToOrders(
            final List<DistributionStep> distribution,
            final MarketSide side,
            final Double sizeScalingFactor,
            final Market market,
            final String partyId
    ) {
        List<Order> orders = distribution.stream().map(d ->
                new Order()
                        .setSize(d.getSize() * sizeScalingFactor)
                        .setPrice(d.getPrice())
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(side)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(TimeInForce.GTC)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).collect(Collectors.toList());
        if(side.equals(MarketSide.BUY)) {
            orders.sort(Comparator.comparing(Order::getPrice).reversed());
        } else {
            orders.sort(Comparator.comparing(Order::getPrice));
        }
        return orders;
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
     * @param bids {@link List<Order>}
     * @param asks {@link List<Order>}
     * @param market {@link Market}
     * @param marketConfig {@link MarketConfig}
     */
    private void updateSpread(
            final List<Order> bids,
            final List<Order> asks,
            final Market market,
            final MarketConfig marketConfig
    ) {
        Order bestBid = bids.get(0);
        Order bestAsk = asks.get(0);
        double makerFee = networkParameterService.getAsInt(MAKER_FEE_PARAM);
        double hedgeSpread = (bestAsk.getPrice() - bestBid.getPrice()) / bestBid.getPrice();
        double liquidityRebate = market.getLiquidityFee() > 0 ?
                -1.0 * market.getLiquidityFee() : market.getLiquidityFee();
        double makerRebate = makerFee > 0 ? -1.0 * makerFee : makerFee;
        double targetSpread = getTargetSpread(hedgeSpread, marketConfig.getTargetEdge(),
                marketConfig.getHedgeFee(), liquidityRebate, makerRebate);
        double currentSpread = (bestAsk.getPrice() - bestBid.getPrice()) / bestBid.getPrice();
        if(currentSpread < targetSpread) {
            double delta = (targetSpread - currentSpread) * bestBid.getPrice() * 0.5;
            bids.forEach(b -> b.setPrice(b.getPrice() - delta));
            asks.forEach(a -> a.setPrice(a.getPrice() + delta));
        }
        log.info("Bid price = {}; Ask price = {}; Spread = {}", bestBid.getPrice(), bestAsk.getPrice(), targetSpread);
    }

    /**
     * Get the best price for the given market and side (using reference mid-price as a back-up)
     *
     * @param side {@link MarketSide}
     * @param market {@link Market}
     *
     * @return the best price
     */
    private double getBestPrice(
            final MarketSide side,
            final Market market
    ) {
        double bestPrice = side.equals(MarketSide.BUY) ?
                market.getBestBidPrice() : market.getBestAskPrice();
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
     * @param orders {@link List<Order>}
     *
     * @return effective total volume
     */
    private double getEffectiveVolume(
            final List<Order> orders
    ) {
        double tauScaling = networkParameterService.getAsDouble(TAU_SCALING_PARAM);
        double minProbabilityOfTrading = networkParameterService.getAsDouble(MIN_PROB_OF_TRADING_PARAM);
        return orders.stream().mapToDouble(order -> {
            Market market = order.getMarket();
            double tau = market.getTau() * tauScaling;
            double bestPrice = getBestPrice(order.getSide(), market);
            double probability = quantUtils.getProbabilityOfTrading(market.getMu(), market.getSigma(), bestPrice, tau,
                    market.getMinValidPrice(), market.getMaxValidPrice(), order.getPrice(), order.getSide());
            return order.getSize() * order.getPrice() * Math.max(minProbabilityOfTrading, probability);
        }).sum();
    }

    /**
     * Scale order sizes when a liquidity commitment exists
     *
     * @param marketId the market ID
     * @param bids {@link List<Order>}
     * @param asks {@link List<Order>}
     * @param tradingConfig {@link TradingConfig}
     */
    private void scaleOrdersForLiquidityCommitment(
            final String marketId,
            final List<Order> bids,
            final List<Order> asks,
            final TradingConfig tradingConfig
    ) {
        Optional<LiquidityCommitment> liquidityCommitmentOptional = liquidityCommitmentStore.getItems().stream()
                .filter(lc -> lc.getMarket().getId().equals(marketId)).findFirst();
        if(liquidityCommitmentOptional.isPresent()) {
            double commitmentAmount = liquidityCommitmentOptional.get().getCommitmentAmount();
            scaleOrders(bids, commitmentAmount, tradingConfig);
            scaleOrders(asks, commitmentAmount, tradingConfig);
        }
    }

    /**
     * Scale up order sizes so that the quotes satisfy the LP commitment amount and pegs are not auto-deployed
     *
     * @param orders {@link List<Order>}
     * @param commitmentAmount the LP commitment amount
     * @param tradingConfig {@link TradingConfig}
     */
    private void scaleOrders(
            final List<Order> orders,
            final double commitmentAmount,
            final TradingConfig tradingConfig
    ) {
        double stakeToSiskas = networkParameterService.getAsDouble(STAKE_TO_SISKAS_PARAM);
        double effectiveVolume = getEffectiveVolume(orders);
        double targetVolume = (commitmentAmount * (1 + tradingConfig.getStakeBuffer())) * stakeToSiskas;
        double volumeRatio = effectiveVolume / targetVolume;
        if(volumeRatio < 1) {
            double modifier = 1.0 / volumeRatio;
            orders.forEach(o -> o.setSize(o.getSize() * modifier));
        }
    }
}
