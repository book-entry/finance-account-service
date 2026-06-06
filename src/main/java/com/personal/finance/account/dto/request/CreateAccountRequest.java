package com.personal.finance.account.dto.request;

import com.personal.finance.account.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Body for {@code POST /v1/accounts} — spec §3.1. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for creating a new bank or credit-card account — spec §3.1.")
public class CreateAccountRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Human-readable name for the account.",
            example = "HSBC Main Savings",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountName;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Name of the bank or financial institution.",
            example = "HSBC Hong Kong",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankName;

    @Size(max = 20)
    @Schema(description = "Bank routing or sort code (optional, max 20 chars).",
            example = "004")
    private String bankCode;

    /** Free text — card number, IBAN, anything. No length cap per spec §1.2. */
    @Schema(description = "Free-text account identifier: card number, IBAN, or any internal reference. No length cap per spec §1.2.",
            example = "4111-1111-1111-1234")
    private String accountCode;

    @NotNull
    @Schema(description = "Broad account type that drives double-entry bookkeeping classification.",
            example = "ASSET",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private AccountType accountType;

    @NotBlank
    @Size(min = 3, max = 3)
    @Schema(description = "ISO 4217 three-letter currency code.",
            example = "HKD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    /** Optional — defaults to zero per spec §1.2. */
    @Schema(description = "Opening balance at account creation time. Defaults to zero when omitted, per spec §1.2.",
            example = "1234.56")
    private BigDecimal openingBalance;

    @Schema(description = "Optional free-text description or notes for the account.",
            example = "Primary day-to-day spending account.")
    private String description;
}
