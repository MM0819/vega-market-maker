package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Position;
import com.vega.protocol.store.PositionStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class PositionServiceTest {

    private PositionService positionService;
    private final PositionStore positionStore = Mockito.mock(PositionStore.class);

    @BeforeEach
    public void setup() {
        positionService = new PositionService(positionStore);
    }

    @Test
    public void testGetExposureLong() {
        Mockito.when(positionStore.getItems()).thenReturn(List.of(
                new Position().setMarket(new Market().setId("1")).setSide(MarketSide.BUY).setSize(10.0),
                new Position().setMarket(new Market().setId("2")).setSide(MarketSide.BUY).setSize(10.0)
        ));
        double exposure = positionService.getExposure("1");
        Assertions.assertEquals(10.0, exposure);
    }

    @Test
    public void testGetExposureShort() {
        Mockito.when(positionStore.getItems()).thenReturn(List.of(
                new Position().setMarket(new Market().setId("1")).setSide(MarketSide.SELL).setSize(10.0),
                new Position().setMarket(new Market().setId("2")).setSide(MarketSide.SELL).setSize(10.0)
        ));
        double exposure = positionService.getExposure("1");
        Assertions.assertEquals(10.0, exposure * -1);
    }

    @Test
    public void testGetExposureZero() {
        Mockito.when(positionStore.getItems()).thenReturn(List.of());
        double exposure = positionService.getExposure("1");
        Assertions.assertEquals(0.0, exposure);
    }

    @Test
    public void testGetExposureWithZeroSize() {
        Mockito.when(positionStore.getItems()).thenReturn(List.of(new Position()
                .setMarket(new Market().setId("1")).setSide(MarketSide.SELL).setSize(0.0)));
        double exposure = positionService.getExposure("1");
        Assertions.assertEquals(0.0, exposure);
    }
}