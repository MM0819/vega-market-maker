package com.vega.protocol.service;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.vega.AccountStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountStore accountStore;

    public AccountService(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    /**
     * Get the total balance for the active market's settlement asset
     *
     * @param settlementAsset the settlement asset
     *
     * @return the user's total balance
     */
    public BigDecimal getTotalBalance(
            final String settlementAsset
    ) {
        return accountStore.getItems().stream().filter(a -> a.getAsset().equals(settlementAsset))
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}