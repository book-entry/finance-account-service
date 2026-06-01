package com.personal.finance.account.dto.response;

import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response body for all account endpoints — spec §3.1. */
@Value
@Builder
public class AccountResponse {
    UUID accountId;
    String accountName;
    String bankName;
    String bankCode;
    String accountCode;
    AccountType accountType;
    String currency;
    BigDecimal openingBalance;
    AccountStatus status;
    String description;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
