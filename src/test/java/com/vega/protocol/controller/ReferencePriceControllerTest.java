package com.vega.protocol.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {ReferencePriceController.class, ReferencePriceStore.class})
@WebMvcTest
public class ReferencePriceControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReferencePriceStore store;

    @Test
    public void testGetReferencePrice() throws Exception {
        ReferencePrice referencePrice = new ReferencePrice()
                .setMidPrice(BigDecimal.ONE)
                .setBidPrice(BigDecimal.ONE)
                .setAskPrice(BigDecimal.ONE)
                .setBidSize(BigDecimal.ONE)
                .setAskSize(BigDecimal.ONE);
        store.update(referencePrice);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/reference-price"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        ReferencePrice price = new ObjectMapper().readValue(body, ReferencePrice.class);
        Assertions.assertEquals(referencePrice, price);
    }
}