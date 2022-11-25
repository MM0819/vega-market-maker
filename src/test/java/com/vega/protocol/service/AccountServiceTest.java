package com.vega.protocol.service;

import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
        var asset = TestingHelper.getAsset(USDT);
        Mockito.when(store.getAssetById(USDT)).thenReturn(Optional.of(asset));
        Mockito.when(decimalUtils.convertToDecimals(0, new BigDecimal("1"))).thenReturn(1.0);
        Mockito.when(decimalUtils.convertToDecimals(0, new BigDecimal("10"))).thenReturn(10.0);
        Mockito.when(store.getAccountsByAsset(USDT)).thenReturn(List.of(
                TestingHelper.getAccount(market.getId(), "10", USDT),
                TestingHelper.getAccount(market.getId(), "1", USDT)));
        double balance = accountService.getTotalBalance(USDT);
        Assertions.assertEquals(11, balance);
    }
}