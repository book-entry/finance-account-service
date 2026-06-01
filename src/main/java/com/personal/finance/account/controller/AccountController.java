package com.personal.finance.account.controller;

import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.request.UpdateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.dto.response.ValidateAccountsResponse;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import com.personal.finance.account.service.AccountService;
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
public class AccountController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final AccountService accountService;

    /** Spec §3.1 — {@code POST /v1/accounts}. Returns 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@RequestHeader(USER_ID_HEADER) String userId,
                                         @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(userId, request);
    }

    /** Spec §3.1 — {@code GET /v1/accounts}. Returns 200. */
    @GetMapping
    public List<AccountResponse> listAccounts(@RequestHeader(USER_ID_HEADER) String userId,
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
    public ValidateAccountsResponse validateAccounts(@RequestHeader(USER_ID_HEADER) String userId,
                                                     @RequestParam("ids") List<UUID> ids) {
        return accountService.validateAccounts(userId, ids);
    }

    /** Spec §3.1 — {@code GET /v1/accounts/{id}}. Returns 200, 404 if not owned. */
    @GetMapping("/{id}")
    public AccountResponse getAccount(@RequestHeader(USER_ID_HEADER) String userId,
                                      @PathVariable("id") UUID id) {
        return accountService.getAccount(userId, id);
    }

    /** Spec §3.1 — {@code PUT /v1/accounts/{id}}. Partial update; returns 200. */
    @PutMapping("/{id}")
    public AccountResponse updateAccount(@RequestHeader(USER_ID_HEADER) String userId,
                                         @PathVariable("id") UUID id,
                                         @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(userId, id, request);
    }

    /** Spec §3.1 — {@code DELETE /v1/accounts/{id}}. Returns 204; sets status=CLOSED. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeAccount(@RequestHeader(USER_ID_HEADER) String userId,
                             @PathVariable("id") UUID id) {
        accountService.closeAccount(userId, id);
    }
}
