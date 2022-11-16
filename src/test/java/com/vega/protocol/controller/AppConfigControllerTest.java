package com.vega.protocol.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.GlobalExceptionHandler;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.response.ErrorResponse;
import com.vega.protocol.service.AppConfigService;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {AppConfigController.class, AppConfigService.class, AppConfigStore.class})
@WebMvcTest
@Import(GlobalExceptionHandler.class)
public class AppConfigControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AppConfigStore store;

    private final ObjectMapper mapper = new ObjectMapper();

    private AppConfig getAppConfig() {
        return new AppConfig()
                .setFee(0.001)
                .setMinSpread(0.003)
                .setMaxSpread(0.01)
                .setOrderCount(10)
                .setBidSizeFactor(1.0)
                .setBidQuoteRange(0.05)
                .setAskSizeFactor(1.0)
                .setAskQuoteRange(0.05);
    }

    @BeforeEach
    public void setup() {
        store.update(getAppConfig());
    }

    @Test
    public void testGetAppConfig() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/app-config"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        AppConfig config = mapper.readValue(body, AppConfig.class);
        Assertions.assertEquals(config, getAppConfig());
        Assertions.assertTrue(store.get().isPresent());
        Assertions.assertEquals(store.get().get(), config);
    }

    @Test
    public void testGetAppConfigMissing() throws Exception {
        store.update(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/app-config"))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.APP_CONFIG_NOT_FOUND)));
    }

    @Test
    public void testUpdateAppConfig() throws Exception {
        AppConfig config = getAppConfig()
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

    @Test
    public void testUpdateAppConfigMissingFee() throws Exception {
        AppConfig config = getAppConfig()
                .setFee(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.FEE_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingOrderCount() throws Exception {
        AppConfig config = getAppConfig()
                .setOrderCount(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.ORDER_COUNT_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingMinSpread() throws Exception {
        AppConfig config = getAppConfig()
                .setMinSpread(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.MIN_SPREAD_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingMaxSpread() throws Exception {
        AppConfig config = getAppConfig()
                .setMaxSpread(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.MAX_SPREAD_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingBidQuoteRange() throws Exception {
        AppConfig config = getAppConfig()
                .setBidQuoteRange(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.BID_QUOTE_RANGE_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingAskQuoteRange() throws Exception {
        AppConfig config = getAppConfig()
                .setAskQuoteRange(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.ASK_QUOTE_RANGE_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingBidSizeFactor() throws Exception {
        AppConfig config = getAppConfig()
                .setBidSizeFactor(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.BID_SIZE_FACTOR_MANDATORY)));
    }

    @Test
    public void testUpdateAppConfigMissingAskSizeFactor() throws Exception {
        AppConfig config = getAppConfig()
                .setAskSizeFactor(null);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/app-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(config)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Assertions.assertEquals(body, mapper.writeValueAsString(new ErrorResponse()
                .setError(ErrorCode.ASK_SIZE_FACTOR_MANDATORY)));
    }
}