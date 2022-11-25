package com.vega.protocol.service;

import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import com.vega.protocol.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Vega;

@Slf4j
public class OrderServiceTest {

    private OrderService orderService;
    private VegaStore vegaStore;
    private ReferencePriceStore referencePriceStore;
    private LiquidityProvisionService liquidityProvisionService;
    private AssetService assetService;
    private MarketService marketService;
    private NetworkParameterService networkParameterService;
    private PositionService positionService;
    private DecimalUtils decimalUtils;
    private QuantUtils quantUtils;
    private PricingUtils pricingUtils;
    private SleepUtils sleepUtils;
    private VegaGrpcClient vegaGrpcClient;

    @BeforeEach
    public void setup() {
        vegaStore = Mockito.mock(VegaStore.class);
        referencePriceStore = Mockito.mock(ReferencePriceStore.class);
        liquidityProvisionService = Mockito.mock(LiquidityProvisionService.class);
        assetService = Mockito.mock(AssetService.class);
        marketService = Mockito.mock(MarketService.class);
        networkParameterService = Mockito.mock(NetworkParameterService.class);
        positionService = Mockito.mock(PositionService.class);
        decimalUtils = Mockito.mock(DecimalUtils.class);
        quantUtils = Mockito.mock(QuantUtils.class);
        pricingUtils = Mockito.mock(PricingUtils.class);
        sleepUtils = Mockito.mock(SleepUtils.class);
        vegaGrpcClient = Mockito.mock(VegaGrpcClient.class);
        orderService = new OrderService(vegaStore, referencePriceStore, liquidityProvisionService, assetService,
                marketService, networkParameterService, positionService, decimalUtils, quantUtils, pricingUtils,
                sleepUtils, vegaGrpcClient);
    }

    @Test
    public void testGetOtherSide() {
        Assertions.assertEquals(Vega.Side.SIDE_SELL, orderService.getOtherSide(Vega.Side.SIDE_BUY));
        Assertions.assertEquals(Vega.Side.SIDE_BUY, orderService.getOtherSide(Vega.Side.SIDE_SELL));
    }
}