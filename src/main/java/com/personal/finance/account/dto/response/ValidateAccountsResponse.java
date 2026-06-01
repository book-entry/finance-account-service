package com.personal.finance.account.dto.response;

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
public class ValidateAccountsResponse {
    List<UUID> valid;
    List<UUID> invalid;
}
