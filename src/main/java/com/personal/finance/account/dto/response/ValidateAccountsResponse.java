package com.personal.finance.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Response for {@code GET /v1/accounts/validate} — spec §3.1 internal endpoint.
 * Bulk Upload Service uses {@code invalid} to build per-row errors.
 */
@Value
@Builder
@Schema(description = "Result of bulk account-id validation for the internal /validate endpoint — spec §3.1. " +
        "Consumed by finance-ingestion-service to classify per-row errors in bulk uploads.")
public class ValidateAccountsResponse {

    @Schema(description = "Account UUIDs from the request that are owned by the user and currently ACTIVE.",
            example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"b2c3d4e5-f6a7-8901-bcde-f12345678901\"]")
    List<UUID> valid;

    @Schema(description = "Account UUIDs from the request that are not owned by the user, do not exist, or are CLOSED.",
            example = "[\"c3d4e5f6-a7b8-9012-cdef-123456789012\"]")
    List<UUID> invalid;
}
