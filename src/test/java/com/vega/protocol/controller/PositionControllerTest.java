package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.Position;
import com.vega.protocol.store.vega.PositionStore;
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
@ContextConfiguration(classes = {PositionController.class, PositionStore.class})
@WebMvcTest
public class PositionControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private PositionStore store;

    @Test
    public void testGetPositions() throws Exception {
        Position position = new Position().setId("12345");
        store.update(position);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/position"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Position> positions = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(positions.size(), 1);
        Assertions.assertEquals(positions.get(0).getId(), "12345");
    }
}