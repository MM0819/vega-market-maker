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
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.NetworkParameterStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final NetworkParameterStore networkParameterStore;
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
                            NetworkParameterStore networkParameterStore,
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
        this.networkParameterStore = networkParameterStore;
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
        BigDecimal balance = accountService.getTotalBalance(market.getSettlementAsset());
        if(balance.doubleValue() == 0) {
            log.info("Cannot update quotes because balance = {}", balance);
            return;
        }
        BigDecimal exposure = positionService.getExposure(marketId);
        // TODO - we should use the net exposure here after considering our hedge on Binance / IG
        TradingConfig tradingConfig = tradingConfigRepository.findByMarketConfig(marketConfig)
                .orElseThrow(() -> new TradingException(ErrorCode.TRADING_CONFIG_NOT_FOUND));
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        BigDecimal midPrice = referencePrice.getMidPrice();
        BigDecimal bidPoolSize = balance.multiply(BigDecimal.valueOf(0.5));
        BigDecimal askPoolSize = bidPoolSize.divide(midPrice, market.getDecimalPlaces(), RoundingMode.HALF_DOWN);
        double openVolumeRatio = Math.min(0.99, exposure.abs().doubleValue() / askPoolSize.doubleValue());
        log.info("\n\nReference price = {}\nExposure = {}\nBid pool size = {}\nAsk pool size = {}\n",
                referencePrice, exposure, bidPoolSize, askPoolSize);
        BigDecimal bidVolume = askPoolSize.multiply(BigDecimal.valueOf(tradingConfig.getCommitmentBalanceRatio()));
        BigDecimal askVolume = askPoolSize.multiply(BigDecimal.valueOf(tradingConfig.getCommitmentBalanceRatio()));
        double bidQuoteRange = tradingConfig.getBidQuoteRange();
        double askQuoteRange = tradingConfig.getAskQuoteRange();
        if(exposure.doubleValue() > 0) {
            bidVolume = bidVolume.multiply(BigDecimal.valueOf(1 - openVolumeRatio));
        } else if(exposure.doubleValue() < 0) {
            askVolume = askVolume.multiply(BigDecimal.valueOf(1 - openVolumeRatio));
        }
        List<DistributionStep> askDistribution = pricingUtils.getDistribution(
                referencePrice.getAskPrice().doubleValue(), askVolume.doubleValue(), askQuoteRange,
                MarketSide.SELL, tradingConfig.getQuoteOrderCount());
        List<DistributionStep> bidDistribution = pricingUtils.getDistribution(
                referencePrice.getBidPrice().doubleValue(), bidVolume.doubleValue(), bidQuoteRange,
                MarketSide.BUY, tradingConfig.getQuoteOrderCount());
        if(bidDistribution.size() == 0) {
            log.warn("Bid distribution was empty !!");
            return;
        }
        if(askDistribution.size() == 0) {
            log.warn("Ask distribution was empty !!");
            return;
        }
        List<Order> bids = bidDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * tradingConfig.getBidSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.BUY)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(TimeInForce.GTC)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).sorted(Comparator.comparing(Order::getPrice).reversed()).collect(Collectors.toList());
        List<Order> asks = askDistribution.stream().map(d ->
                new Order()
                        .setSize(BigDecimal.valueOf(d.getSize() * tradingConfig.getAskSizeFactor()))
                        .setPrice(BigDecimal.valueOf(d.getPrice()))
                        .setStatus(OrderStatus.ACTIVE)
                        .setSide(MarketSide.SELL)
                        .setType(OrderType.LIMIT)
                        .setTimeInForce(TimeInForce.GTC)
                        .setMarket(market)
                        .setPartyId(partyId)
        ).sorted(Comparator.comparing(Order::getPrice)).collect(Collectors.toList());
        Order bestBid = bids.get(0);
        Order bestAsk = asks.get(0);
        NetworkParameter makerFeeParam = networkParameterStore.getById(MAKER_FEE_PARAM)
                .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
        double hedgeSpread = (bestAsk.getPrice().doubleValue() - bestBid.getPrice().doubleValue()) /
                bestBid.getPrice().doubleValue();
        double liquidityRebate = market.getLiquidityFee() > 0 ?
                -1.0 * market.getLiquidityFee() : market.getLiquidityFee();
        double makerRebate = Double.parseDouble(makerFeeParam.getValue()) > 0 ?
                -1.0 * Double.parseDouble(makerFeeParam.getValue()) : Double.parseDouble(makerFeeParam.getValue());
        double targetSpread = getTargetSpread(hedgeSpread, marketConfig.getTargetEdge(),
                marketConfig.getHedgeFee(), liquidityRebate, makerRebate);
        updateSpread(bids, asks, targetSpread);
        log.info("Bid price = {}; Ask price = {}; Spread = {}", bestBid.getPrice(), bestAsk.getPrice(), targetSpread);
        Optional<LiquidityCommitment> liquidityCommitmentOptional = liquidityCommitmentStore.getItems().stream()
                .filter(lc -> lc.getMarket().getId().equals(marketId)).findFirst();
        if(liquidityCommitmentOptional.isPresent()) {
            BigDecimal commitmentAmount = liquidityCommitmentOptional.get().getCommitmentAmount();
            adjustOrders(bids, commitmentAmount, tradingConfig);
            adjustOrders(asks, commitmentAmount, tradingConfig);
        }
        List<Order> submissions = new ArrayList<>();
        submissions.addAll(bids);
        submissions.addAll(asks);
        List<Order> currentOrders = orderStore.getItems().stream().filter(o -> !o.getIsPeggedOrder())
                .filter(o -> o.getStatus().equals(OrderStatus.ACTIVE)).toList();
        List<Order> currentBids = currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.BUY))
                .sorted(Comparator.comparing(Order::getPrice).reversed()).toList();
        List<Order> currentAsks = currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.SELL))
                .sorted(Comparator.comparing(Order::getPrice)).toList();
        if(shouldUpdateQuotes(currentBids, currentAsks, bestBid, bestAsk, tradingConfig)) {
            List<String> cancellations = currentOrders.stream().map(Order::getId).toList();
            NetworkParameter maxBatchSizeParam = networkParameterStore.getById(MAX_BATCH_SIZE_PARAM)
                    .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
            int maxBatchSize = Integer.parseInt(maxBatchSizeParam.getValue());
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
     * @param targetSpread the target spread on Vega
     */
    private void updateSpread(
            final List<Order> bids,
            final List<Order> asks,
            final double targetSpread
    ) {
        Order bestBid = bids.get(0);
        Order bestAsk = asks.get(0);
        double currentSpread = (bestAsk.getPrice().doubleValue() - bestBid.getPrice().doubleValue()) /
                bestBid.getPrice().doubleValue();
        if(currentSpread < targetSpread) {
            double delta = (targetSpread - currentSpread) * bestBid.getPrice().doubleValue() * 0.5;
            bids.forEach(b -> b.setPrice(b.getPrice().subtract(BigDecimal.valueOf(delta))));
            asks.forEach(a -> a.setPrice(a.getPrice().add(BigDecimal.valueOf(delta))));
        }
    }

    /**
     * Calculate the effective volume implied by orders after considering probability of a price-level trading
     *
     * @param orders {@link List<Order>}
     *
     * @return effective total volume
     */
    private BigDecimal getEffectiveVolume(
            final List<Order> orders
    ) {
        NetworkParameter tauScalingParam = networkParameterStore.getById(TAU_SCALING_PARAM)
                .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
        ReferencePrice referencePrice = referencePriceStore.get()
                .orElseThrow(() -> new TradingException(ErrorCode.REFERENCE_PRICE_NOT_FOUND));
        return orders.stream().map(o -> {
            double mu = o.getMarket().getMu();
            double tau = o.getMarket().getTau() * new BigDecimal(tauScalingParam.getValue()).doubleValue();
            double sigma = o.getMarket().getSigma();
            double bestPrice = o.getSide().equals(MarketSide.BUY) ? o.getMarket().getBestBidPrice().doubleValue() :
                    o.getMarket().getBestAskPrice().doubleValue();
            if(bestPrice == 0) {
                bestPrice = referencePrice.getMidPrice().doubleValue();
            }
            double minValidPrice = o.getMarket().getMinValidPrice().doubleValue();
            double maxValidPrice = o.getMarket().getMaxValidPrice().doubleValue();
            double price = o.getPrice().doubleValue();
            MarketSide side = o.getSide();
            double probability = quantUtils.getProbabilityOfTrading(mu, sigma, bestPrice, tau,
                    minValidPrice, maxValidPrice, price, side);
            NetworkParameter minProbabilityOfTradingParam = networkParameterStore.getById(MIN_PROB_OF_TRADING_PARAM)
                    .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
            probability = Math.max(new BigDecimal(minProbabilityOfTradingParam.getValue()).doubleValue(), probability);
            return o.getSize().multiply(o.getPrice()).multiply(BigDecimal.valueOf(probability));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Scale up order sizes so that the quotes satisfy the LP commitment amount and pegs are not auto-deployed
     *
     * @param orders {@link List<Order>}
     * @param commitmentAmount the LP commitment amount
     * @param tradingConfig {@link TradingConfig}
     */
    private void adjustOrders(
            final List<Order> orders,
            final BigDecimal commitmentAmount,
            final TradingConfig tradingConfig
    ) {
        NetworkParameter stakeToSiskasParam = networkParameterStore.getById(STAKE_TO_SISKAS_PARAM)
                .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
        BigDecimal effectiveVolume = getEffectiveVolume(orders);
        BigDecimal targetVolume = (commitmentAmount.multiply(BigDecimal.valueOf(1 + tradingConfig.getStakeBuffer())))
                .multiply(new BigDecimal(stakeToSiskasParam.getValue()));
        BigDecimal volumeRatio = effectiveVolume.divide(targetVolume, 8, RoundingMode.HALF_DOWN);
        if(volumeRatio.doubleValue() < 1) {
            BigDecimal modifier = BigDecimal.ONE.divide(volumeRatio, 8, RoundingMode.HALF_DOWN);
            orders.forEach(o -> o.setSize(o.getSize().multiply(modifier)));
        }
    }

    /**
     * Check whether the price has changed sufficiently to justify updating our quotes
     *
     * @param currentBids the current bids
     * @param currentAsks the current asks
     * @param bestBid the new best bid
     * @param bestAsk the new best ask
     * @param tradingConfig {@link TradingConfig}
     *
     * @return true / false
     */
    private boolean shouldUpdateQuotes(
            final List<Order> currentBids,
            final List<Order> currentAsks,
            final Order bestBid,
            final Order bestAsk,
            final TradingConfig tradingConfig
    ) {
        /*if(currentBids.size() > 0 && currentAsks.size() > 0) {
            Order currentBestBid = currentBids.get(0);
            Order currentBestAsk = currentAsks.get(0);
            BigDecimal staticMidPrice = (bestBid.getPrice().add(bestAsk.getPrice()))
                    .multiply(BigDecimal.valueOf(0.5));
            BigDecimal currentMidPrice = (currentBestBid.getPrice().add(currentBestAsk.getPrice()))
                    .multiply(BigDecimal.valueOf(0.5));
            double priceDelta = ((staticMidPrice.subtract(currentMidPrice).abs())
                    .divide(currentMidPrice, 4, RoundingMode.HALF_DOWN)).doubleValue();
            if(priceDelta < (config.getMinSpread() / 2.0)) {
                log.warn("Not updating quotes because the mid-price delta is only = {}%",
                        Math.round(priceDelta * 10000.0) / 100.0);
                return false;
            }
        }*/
        return true;
    }
}
