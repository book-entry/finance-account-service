package com.personal.finance.account.service;

import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.request.UpdateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.dto.response.ValidateAccountsResponse;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Implements the account flows defined in spec §3.1. */
public interface AccountService {

    /** Spec §3.1 POST /v1/accounts. */
    AccountResponse createAccount(String userId, CreateAccountRequest request);

    /** Spec §3.1 GET /v1/accounts — list with optional status + type filters. */
    List<AccountResponse> listAccounts(String userId, AccountStatus status, AccountType type);

    /** Spec §3.1 GET /v1/accounts/{id}. */
    AccountResponse getAccount(String userId, UUID accountId);

    /** Spec §3.1 PUT /v1/accounts/{id} — partial update. */
    AccountResponse updateAccount(String userId, UUID accountId, UpdateAccountRequest request);

    /** Spec §3.1 DELETE /v1/accounts/{id} — sets status=CLOSED. */
    void closeAccount(String userId, UUID accountId);

    /** Spec §3.1 GET /v1/accounts/validate — internal batch validation. */
    ValidateAccountsResponse validateAccounts(String userId, Collection<UUID> accountIds);
}
