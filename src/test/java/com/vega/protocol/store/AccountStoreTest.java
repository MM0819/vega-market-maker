package com.vega.protocol.store;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.vega.AccountStore;

public class AccountStoreTest extends MultipleItemStoreTest<Account, AccountStore> {

    private final AccountStore store = new AccountStore();

    @Override
    public AccountStore getStore() {
        return store;
    }

    @Override
    public Account getItem() {
        return new Account();
    }
}
