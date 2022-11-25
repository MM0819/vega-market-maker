package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import datanode.api.v2.TradingData;
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
@ContextConfiguration(classes = {AccountController.class, VegaStore.class})
@WebMvcTest
public class AccountControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private VegaStore store;

    @Test
    public void testGetAccounts() throws Exception {
        var account = TestingHelper.getAccount("12345", "100", "USDT");
        store.updateAccount(account);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/account"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<TradingData.AccountBalance> accounts = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(accounts.size(), 1);
        Assertions.assertEquals(accounts.get(0).getMarketId(), "12345");
    }
}