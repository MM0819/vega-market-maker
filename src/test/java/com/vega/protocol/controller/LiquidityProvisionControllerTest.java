package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import vega.Markets;
import vega.Vega;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {LiquidityProvisionController.class, VegaStore.class})
@WebMvcTest
public class LiquidityProvisionControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private VegaStore store;

    @Test
    public void testGetLiquidityProvision() throws Exception {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, "USDT");
        var liquidityProvision = TestingHelper.getLiquidityProvision("1", market);
        store.updateLiquidityProvision(liquidityProvision);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/liquidity-provision"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Vega.LiquidityProvision> provisions = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertTrue(provisions.size() > 0);
        Assertions.assertEquals(provisions.get(0).getId(), TestingHelper.ID);
    }
}