package com.vega.protocol.service;

import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PositionServiceTest {

    private PositionService positionService;
    private final VegaStore vegaStore = Mockito.mock(VegaStore.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);

    @BeforeEach
    public void setup() {
        positionService = new PositionService(vegaStore, decimalUtils);
    }

    @Test
    public void testGetExposureLong() {
        Mockito.when(vegaStore.getPositionByMarketIdAndPartyId("1", "1")).thenReturn(Optional.of(
                TestingHelper.getPosition(100L, "1", "1", TestingHelper.ID)
        ));
        Mockito.when(vegaStore.getMarketById("1")).thenReturn(Optional.of(
                TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                        Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT"))
        );
        Mockito.when(decimalUtils.convertToDecimals(0, new BigDecimal("100"))).thenReturn(100.0);
        double exposure = positionService.getExposure("1", "1");
        Assertions.assertEquals(100.0, exposure);
    }

    @Test
    public void testGetExposureShort() {
        Mockito.when(vegaStore.getPositionByMarketIdAndPartyId("1", "1")).thenReturn(Optional.of(
                TestingHelper.getPosition(-100L, "1", "1", TestingHelper.ID)
        ));
        Mockito.when(vegaStore.getMarketById("1")).thenReturn(Optional.of(
                TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                        Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT"))
        );
        Mockito.when(decimalUtils.convertToDecimals(0, new BigDecimal("-100"))).thenReturn(-100.0);
        double exposure = positionService.getExposure("1", "1");
        Assertions.assertEquals(-100.0, exposure);
    }

    @Test
    public void testGetExposureZero() {
        Mockito.when(vegaStore.getPositions()).thenReturn(Collections.emptyList());
        double exposure = positionService.getExposure("1", "1");
        Assertions.assertEquals(0.0, exposure);
    }

    @Test
    public void testGetExposureWithZeroSize() {
        Mockito.when(vegaStore.getPositions()).thenReturn(List.of(
                TestingHelper.getPosition(0L, "1", "1", TestingHelper.ID),
                TestingHelper.getPosition(0L, "1", "2", TestingHelper.ID)
        ));
        double exposure = positionService.getExposure("1", "1");
        Assertions.assertEquals(0.0, exposure);
    }
}