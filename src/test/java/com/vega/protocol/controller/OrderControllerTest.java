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
import vega.Vega;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {OrderController.class, VegaStore.class})
@WebMvcTest
public class OrderControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private VegaStore store;

    private void getOrders(
            final String path
    ) throws Exception {
        var order = TestingHelper.getOrder(TestingHelper.ID, "100", 1L,
                Vega.Side.SIDE_BUY, Vega.Order.Status.STATUS_ACTIVE, null);
        store.updateOrder(order);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(path))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Vega.Order> orders = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(orders.size(), 1);
        Assertions.assertEquals(orders.get(0).getId(), TestingHelper.ID);
    }

    @Test
    public void testGetOrders() throws Exception {
        getOrders("/order");
    }

    @Test
    public void testGetOrdersByStatus() throws Exception {
        getOrders("/order/status/ACTIVE");
    }
}