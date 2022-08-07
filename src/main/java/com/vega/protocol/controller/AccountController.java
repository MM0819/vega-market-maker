package com.vega.protocol.controller;

import com.vega.protocol.model.Account;
import com.vega.protocol.store.AccountStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountStore accountStore;

    public AccountController(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    @GetMapping
    public ResponseEntity<List<Account>> get() {
        return ResponseEntity.ok(accountStore.getItems());
    }
}