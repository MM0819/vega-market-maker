package com.vega.protocol.service;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.AccountStore;
import org.springframework.stereotype.Service;

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
    public double getTotalBalance(
            final String settlementAsset
    ) {
        return accountStore.getItems().stream().filter(a -> a.getAsset().equals(settlementAsset))
                .mapToDouble(Account::getBalance).sum();
    }
}