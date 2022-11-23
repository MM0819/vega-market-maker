package com.vega.protocol.service;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.AccountStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class AccountServiceTest {

    private static final String USDT = "USDT";
    private static final String USDC = "USDC";

    private AccountService accountService;
    private final AccountStore accountStore = Mockito.mock(AccountStore.class);

    @BeforeEach
    public void setup() {
        accountService = new AccountService(accountStore);
    }

    @Test
    public void testGetTotalBalance() {
        Mockito.when(accountStore.getItems()).thenReturn(List.of(
                new Account().setBalance(10).setAsset(USDT),
                new Account().setBalance(1).setAsset(USDT),
                new Account().setBalance(10).setAsset(USDC),
                new Account().setBalance(1).setAsset(USDC)));
        double balance = accountService.getTotalBalance(USDT);
        Assertions.assertEquals(11, balance);
    }
}