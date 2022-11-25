package com.vega.protocol.service;

import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final VegaStore vegaStore;
    private final DecimalUtils decimalUtils;

    public AccountService(VegaStore vegaStore, DecimalUtils decimalUtils) {
        this.vegaStore = vegaStore;
        this.decimalUtils = decimalUtils;
    }

    /**
     * Get the total balance for the active market's settlement asset
     *
     * @param settlementAsset the settlement asset
     *
     * @return the user's total balance
     */
    public double getTotalBalance(
            final String settlementAsset
    ) {
        AtomicReference<Double> balance = new AtomicReference<>((double) 0);
        vegaStore.getAssetById(settlementAsset).ifPresent(asset -> {
            var accounts = vegaStore.getAccountsByAsset(asset.getDetails().getSymbol());
            long dp = asset.getDetails().getDecimals();
            var balances = accounts.stream()
                    .map(a -> decimalUtils.convertToDecimals(dp, new BigDecimal(a.getBalance()))).toList();
            balance.set(balances.stream().mapToDouble(Double::doubleValue).sum());
        });
        return balance.get();
    }
}