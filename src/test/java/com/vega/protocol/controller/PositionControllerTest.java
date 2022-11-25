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
@ContextConfiguration(classes = {PositionController.class, VegaStore.class})
@WebMvcTest
public class PositionControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private VegaStore store;

    @Test
    public void testGetPositions() throws Exception {
        var position = TestingHelper.getPosition(1L, "100", "1", "1");
        store.updatePosition(position);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/position"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Vega.Position> positions = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(positions.size(), 1);
        Assertions.assertEquals(positions.get(0).getMarketId(), "1");
        Assertions.assertEquals(positions.get(0).getPartyId(), "1");
    }
}