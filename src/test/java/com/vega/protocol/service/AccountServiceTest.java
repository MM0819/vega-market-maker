package com.vega.protocol.service;

import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.util.List;

public class AccountServiceTest {

    private static final String USDT = "USDT";
    private static final String USDC = "USDC";

    private AccountService accountService;
    private final VegaStore store = Mockito.mock(VegaStore.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);

    @BeforeEach
    public void setup() {
        accountService = new AccountService(store, decimalUtils);
    }

    @Test
    public void testGetTotalBalance() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        Mockito.when(store.getAccountsByAsset(USDT)).thenReturn(List.of(
                TestingHelper.getAccount(market.getId(), "10", USDT),
                TestingHelper.getAccount(market.getId(), "10", USDC),
                TestingHelper.getAccount(market.getId(), "1", USDT),
                TestingHelper.getAccount(market.getId(), "1", USDC)));
        double balance = accountService.getTotalBalance(USDT);
        Assertions.assertEquals(11, balance);
    }
}