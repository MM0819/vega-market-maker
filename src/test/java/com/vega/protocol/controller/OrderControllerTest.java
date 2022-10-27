package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.vega.OrderStore;
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
@ContextConfiguration(classes = {OrderController.class, OrderStore.class})
@WebMvcTest
public class OrderControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private OrderStore store;

    @Test
    public void testGetOrders() throws Exception {
        Order order = new Order().setId("12345");
        store.update(order);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/order"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Order> orders = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(orders.size(), 1);
        Assertions.assertEquals(orders.get(0).getId(), "12345");
    }

    @Test
    public void testGetOrdersByStatus() throws Exception {
        Order order = new Order().setId("12345").setStatus(OrderStatus.ACTIVE);
        store.update(order);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/order/status/ACTIVE"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Order> orders = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(orders.size(), 1);
        Assertions.assertEquals(orders.get(0).getId(), "12345");
    }
}