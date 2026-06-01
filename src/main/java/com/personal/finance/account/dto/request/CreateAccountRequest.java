package com.personal.finance.account.dto.request;

import com.personal.finance.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Body for {@code POST /v1/accounts} — spec §3.1. */
@Data
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank
    @Size(max = 255)
    private String accountName;

    @NotBlank
    @Size(max = 255)
    private String bankName;

    @Size(max = 20)
    private String bankCode;

    /** Free text — card number, IBAN, anything. No length cap per spec §1.2. */
    private String accountCode;

    @NotNull
    private AccountType accountType;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    /** Optional — defaults to zero per spec §1.2. */
    private BigDecimal openingBalance;

    private String description;
}
