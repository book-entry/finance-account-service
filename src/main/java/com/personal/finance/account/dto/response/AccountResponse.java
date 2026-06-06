package com.personal.finance.account.dto.response;

import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response body for all account endpoints — spec §3.1. */
@Value
@Builder
@Schema(description = "Full representation of a bank or credit-card account returned by all account endpoints — spec §3.1.")
public class AccountResponse {

    @Schema(description = "Unique identifier of the account.", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID accountId;

    @Schema(description = "Human-readable name for the account.", example = "HSBC Main Savings")
    String accountName;

    @Schema(description = "Name of the bank or financial institution.", example = "HSBC Hong Kong")
    String bankName;

    @Schema(description = "Bank routing or sort code.", example = "004")
    String bankCode;

    @Schema(description = "Free-text account identifier: card number, IBAN, or any internal reference.", example = "4111-1111-1111-1234")
    String accountCode;

    @Schema(description = "Broad account type driving double-entry bookkeeping classification.", example = "ASSET")
    AccountType accountType;

    @Schema(description = "ISO 4217 three-letter currency code.", example = "HKD")
    String currency;

    @Schema(description = "Opening balance recorded at account creation time.", example = "1234.56")
    BigDecimal openingBalance;

    @Schema(description = "Current lifecycle status of the account. CLOSED accounts reject new transactions.", example = "ACTIVE")
    AccountStatus status;

    @Schema(description = "Optional free-text description or notes for the account.", example = "Primary day-to-day spending account.")
    String description;

    @Schema(description = "ISO 8601 timestamp when the account was created.", example = "2024-01-15T08:30:00+08:00")
    OffsetDateTime createdAt;

    @Schema(description = "ISO 8601 timestamp when the account was last updated.", example = "2024-06-04T14:00:00+08:00")
    OffsetDateTime updatedAt;
}
