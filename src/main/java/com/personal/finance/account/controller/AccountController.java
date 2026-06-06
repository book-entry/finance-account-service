package com.personal.finance.account.controller;

import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.request.UpdateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.dto.response.ValidateAccountsResponse;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import com.personal.finance.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST entry points for account-service — spec §3.1. Routing only; all logic
 * lives in {@link AccountService}. Every method extracts the {@code X-User-Id}
 * header; missing header → 400 via finance-common's
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank/credit-card account CRUD — spec §3.1.")
public class AccountController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final AccountService accountService;

    /** Spec §3.1 — {@code POST /v1/accounts}. Returns 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new account",
            description = "Creates a new bank or credit-card account for the authenticated user. " +
                    "The account is immediately ACTIVE. Opening balance defaults to zero when omitted. " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully.",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed Bean Validation (e.g. missing accountName, invalid currency length).",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public AccountResponse createAccount(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(userId, request);
    }

    /** Spec §3.1 — {@code GET /v1/accounts}. Returns 200. */
    @GetMapping
    @Operation(
            summary = "List accounts",
            description = "Returns all accounts owned by the authenticated user. " +
                    "Optionally filtered by status and/or type query parameters. " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully (empty array when none match).",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter value supplied (e.g. unknown AccountStatus or AccountType).",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public List<AccountResponse> listAccounts(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestParam(name = "status", required = false) AccountStatus status,
            @RequestParam(name = "type", required = false) AccountType type) {
        return accountService.listAccounts(userId, status, type);
    }

    /**
     * Spec §3.1 — {@code GET /v1/accounts/validate} (internal, used by Bulk
     * Upload Service). Returns 200 with valid / invalid id arrays.
     *
     * <p>Declared BEFORE {@code /{id}} so Spring picks the static path first.
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate account ids",
            description = "Internal endpoint consumed by finance-ingestion-service. " +
                    "Checks which of the supplied account UUIDs are owned by the user and currently ACTIVE. " +
                    "Returns two lists: valid (owned + ACTIVE) and invalid (everything else). " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result returned; check valid/invalid arrays.",
                    content = @Content(schema = @Schema(implementation = ValidateAccountsResponse.class))),
            @ApiResponse(responseCode = "400", description = "ids query parameter is missing or contains a malformed UUID.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public ValidateAccountsResponse validateAccounts(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestParam("ids") List<UUID> ids) {
        return accountService.validateAccounts(userId, ids);
    }

    /** Spec §3.1 — {@code GET /v1/accounts/{id}}. Returns 200, 404 if not owned. */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a single account",
            description = "Retrieves the full detail of one account by its UUID. " +
                    "Returns 404 if the account does not exist or is not owned by the requesting user. " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found and returned.",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the authenticated user.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public AccountResponse getAccount(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable("id") UUID id) {
        return accountService.getAccount(userId, id);
    }

    /** Spec §3.1 — {@code PUT /v1/accounts/{id}}. Partial update; returns 200. */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update an account",
            description = "Applies a partial update to an existing account. Only non-null fields in the " +
                    "request body are written; accountType and currency cannot be changed after creation per spec. " +
                    "Returns 404 if the account does not exist or is not owned by the requesting user. " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated; full updated representation returned.",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed Bean Validation (e.g. accountName exceeds 255 chars).",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the authenticated user.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public AccountResponse updateAccount(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(userId, id, request);
    }

    /** Spec §3.1 — {@code DELETE /v1/accounts/{id}}. Returns 204; sets status=CLOSED. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Close an account",
            description = "Soft-deletes an account by setting its status to CLOSED. " +
                    "Closed accounts reject new transactions. The record is retained for audit purposes. " +
                    "Returns 404 if the account does not exist or is not owned by the requesting user. " +
                    "Spec §3.1.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account closed successfully; no body returned."),
            @ApiResponse(responseCode = "401", description = "X-User-Id header absent or unrecognised by the gateway.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the authenticated user.",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    public void closeAccount(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true,
                    description = "User id forwarded by finance-gateway.")
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable("id") UUID id) {
        accountService.closeAccount(userId, id);
    }
}
