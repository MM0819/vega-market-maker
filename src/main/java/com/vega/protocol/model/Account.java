package com.vega.protocol.model;

import com.vega.protocol.constant.AccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Account extends UniqueItem {
    private String id;
    private String partyId;
    private AccountType type;
    private BigDecimal balance;
    private String asset;
}