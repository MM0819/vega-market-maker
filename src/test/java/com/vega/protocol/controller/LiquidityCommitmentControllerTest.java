package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.LiquidityCommitment;
import com.vega.protocol.store.vega.LiquidityCommitmentStore;
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
@ContextConfiguration(classes = {LiquidityCommitmentController.class, LiquidityCommitmentStore.class})
@WebMvcTest
public class LiquidityCommitmentControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private LiquidityCommitmentStore store;

    @Test
    public void testGetLiquidityCommitment() throws Exception {
        LiquidityCommitment liquidityCommitment = new LiquidityCommitment().setId("12345");
        store.update(liquidityCommitment);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/liquidity-commitment"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<LiquidityCommitment> commitments = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(commitments.get(0).getId(), "12345");
    }
}