package com.personal.finance.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for partially updating an existing account — spec §3.1. " +
        "Only non-null fields are applied. accountType and currency cannot be changed after creation.")
public class UpdateAccountRequest {

    @Size(max = 255)
    @Schema(description = "New human-readable name for the account. Omit to leave unchanged.",
            example = "HSBC Joint Savings")
    private String accountName;

    @Size(max = 255)
    @Schema(description = "New bank or financial institution name. Omit to leave unchanged.",
            example = "HSBC Hong Kong")
    private String bankName;

    @Size(max = 20)
    @Schema(description = "New bank routing or sort code (max 20 chars). Omit to leave unchanged.",
            example = "004")
    private String bankCode;

    @Schema(description = "New free-text account identifier (card number, IBAN, etc.). Omit to leave unchanged.",
            example = "4111-1111-1111-9999")
    private String accountCode;

    @Schema(description = "New optional free-text description or notes. Omit to leave unchanged.",
            example = "Shared account for household expenses.")
    private String description;
}
