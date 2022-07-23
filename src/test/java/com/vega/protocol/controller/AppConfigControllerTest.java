package com.vega.protocol.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {AppConfigController.class, AppConfigStore.class})
@WebMvcTest
public class AppConfigControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AppConfigStore store;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetAppConfig() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/app-config"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        AppConfig config = mapper.readValue(body, AppConfig.class);
        Assertions.assertEquals(config, new AppConfig());
        Assertions.assertTrue(store.get().isPresent());
        Assertions.assertEquals(store.get().get(), config);
    }

    @Test
    public void testUpdateAppConfig() throws Exception {
        AppConfig config = new AppConfig()
                .setFee(0.0005);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        AppConfig updatedConfig = mapper.readValue(body, AppConfig.class);
        Assertions.assertEquals(config, updatedConfig);
        Assertions.assertTrue(store.get().isPresent());
        Assertions.assertEquals(store.get().get(), updatedConfig);
    }
}