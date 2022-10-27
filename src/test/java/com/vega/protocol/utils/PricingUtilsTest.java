package com.vega.protocol.utils;

import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class PricingUtilsTest {

    private AppConfigStore appConfigStore;
    private PricingUtils pricingUtils;

    @BeforeEach
    public void setup() {
        appConfigStore = Mockito.mock(AppConfigStore.class);
        pricingUtils = new PricingUtils(appConfigStore);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig().setPricingStepSize(0.1)));
    }

    @Test
    public void testGetScalingFactor() {
        double scalingFactor = pricingUtils.getScalingFactor(0);
        Assertions.assertEquals(scalingFactor, 1.0);
    }

    @Test
    public void testGetScalingFactorWithOpenVolume() {
        double scalingFactor = pricingUtils.getScalingFactor(0.5);
        Assertions.assertEquals(scalingFactor, 0.5);
    }
}