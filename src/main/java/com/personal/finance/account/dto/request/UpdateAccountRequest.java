package com.personal.finance.account.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code PUT /v1/accounts/{id}} — spec §3.1. All fields optional;
 * only provided (non-null) fields are applied. {@code accountType} and
 * {@code currency} are intentionally absent — they cannot change after
 * creation per spec.
 */
@Data
@NoArgsConstructor
public class UpdateAccountRequest {

    @Size(max = 255)
    private String accountName;

    @Size(max = 255)
    private String bankName;

    @Size(max = 20)
    private String bankCode;

    private String accountCode;

    private String description;
}
