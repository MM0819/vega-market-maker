package com.vega.protocol.service;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.vega.AccountStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
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
                new Account().setBalance(BigDecimal.TEN).setAsset(USDT),
                new Account().setBalance(BigDecimal.ONE).setAsset(USDT),
                new Account().setBalance(BigDecimal.TEN).setAsset(USDC),
                new Account().setBalance(BigDecimal.ONE).setAsset(USDC)));
        BigDecimal balance = accountService.getTotalBalance(USDT);
        Assertions.assertEquals(BigDecimal.valueOf(11), balance);
    }
}