package com.vega.protocol.store;

import com.vega.protocol.model.Account;
import com.vega.protocol.model.Market;
import org.springframework.stereotype.Repository;

@Repository
public class AccountStore extends MultipleItemStore<Account> {
}