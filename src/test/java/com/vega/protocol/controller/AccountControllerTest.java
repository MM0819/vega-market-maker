package com.vega.protocol.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.protocol.model.Account;
import com.vega.protocol.store.AccountStore;
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
@ContextConfiguration(classes = {AccountController.class, AccountStore.class})
@WebMvcTest
public class AccountControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountStore store;

    @Test
    public void testGetAccounts() throws Exception {
        Account account = new Account().setId("12345");
        store.update(account);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/account"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Account> accounts = new ObjectMapper().readValue(body, new TypeReference<>() {});
        Assertions.assertEquals(accounts.size(), 1);
        Assertions.assertEquals(accounts.get(0).getId(), "12345");
    }
}