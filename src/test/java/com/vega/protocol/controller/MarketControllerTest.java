package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.Market;
import com.vega.protocol.store.vega.MarketStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {MarketController.class, MarketStore.class})
@WebMvcTest
public class MarketControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MarketStore store;

    @Test
    public void testGetMarkets() throws Exception {
        Market market = new Market().setId("12345");
        store.update(market);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/market"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Market> markets = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(markets.size(), 1);
        Assertions.assertEquals(markets.get(0).getId(), "12345");
    }
}