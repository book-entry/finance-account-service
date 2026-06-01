package com.personal.finance.account.service;

import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.request.UpdateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.dto.response.ValidateAccountsResponse;
import com.personal.finance.account.entity.Account;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import com.personal.finance.account.exception.AccountNotFoundException;
import com.personal.finance.account.exception.InvalidCurrencyException;
import com.personal.finance.account.mapper.AccountMapper;
import com.personal.finance.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Spec §3.1 POST /v1/accounts — High-Level Logic:
     * <ol>
     *   <li>Validate currency is a valid ISO 4217 code.</li>
     *   <li>INSERT into accounts with user_id, all fields, status=ACTIVE.</li>
     *   <li>Return 201 with created account row.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AccountResponse createAccount(String userId, CreateAccountRequest request) {
        String currency = normaliseCurrency(request.getCurrency());
        assertValidIsoCurrency(currency);

        Account entity = accountMapper.toEntity(request);
        entity.setUserId(userId);
        entity.setCurrency(currency);
        entity.setStatus(AccountStatus.ACTIVE);
        // Spec §1.2 default — 0 if not supplied.
        if (entity.getOpeningBalance() == null) {
            entity.setOpeningBalance(BigDecimal.ZERO);
        }

        Account saved = accountRepository.save(entity);
        log.info("Account created uid=[{}] id=[{}]", userId, saved.getAccountId());
        return accountMapper.toResponse(saved);
    }

    /**
     * Spec §3.1 GET /v1/accounts — High-Level Logic:
     * <ol>
     *   <li>SELECT WHERE user_id=? AND status=? (default ACTIVE).</li>
     *   <li>Apply optional account_type filter.</li>
     *   <li>Return array (empty if none).</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(String userId, AccountStatus status, AccountType type) {
        AccountStatus effectiveStatus = status == null ? AccountStatus.ACTIVE : status;
        List<Account> rows = type == null
                ? accountRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, effectiveStatus)
                : accountRepository.findByUserIdAndStatusAndAccountTypeOrderByCreatedAtDesc(
                        userId, effectiveStatus, type);
        return rows.stream().map(accountMapper::toResponse).toList();
    }

    /**
     * Spec §3.1 GET /v1/accounts/{id} — High-Level Logic:
     * <ol>
     *   <li>SELECT WHERE account_id=? AND user_id=?.</li>
     *   <li>Return 404 if no row — do not reveal existence to non-owners.</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String userId, UUID accountId) {
        Account account = loadOwned(userId, accountId);
        return accountMapper.toResponse(account);
    }

    /**
     * Spec §3.1 PUT /v1/accounts/{id} — High-Level Logic:
     * <ol>
     *   <li>SELECT scoped to user. 404 if not found.</li>
     *   <li>Apply only non-null fields (account_type / currency NEVER touched).</li>
     *   <li>updated_at refreshed by @UpdateTimestamp on save.</li>
     *   <li>Return 200 with updated row.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AccountResponse updateAccount(String userId, UUID accountId, UpdateAccountRequest request) {
        Account account = loadOwned(userId, accountId);
        accountMapper.applyUpdate(request, account);
        Account saved = accountRepository.save(account);
        log.info("Account updated uid=[{}] id=[{}]", userId, accountId);
        return accountMapper.toResponse(saved);
    }

    /**
     * Spec §3.1 DELETE /v1/accounts/{id} — High-Level Logic:
     * <ol>
     *   <li>SELECT scoped to user. 404 if not found.</li>
     *   <li>UPDATE status=CLOSED — does NOT delete the row.</li>
     *   <li>Return 204. Closed accounts reject new transactions downstream.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void closeAccount(String userId, UUID accountId) {
        Account account = loadOwned(userId, accountId);
        if (account.getStatus() != AccountStatus.CLOSED) {
            account.setStatus(AccountStatus.CLOSED);
            accountRepository.save(account);
            log.info("Account closed uid=[{}] id=[{}]", userId, accountId);
        }
    }

    /**
     * Spec §3.1 GET /v1/accounts/validate — High-Level Logic:
     * <ol>
     *   <li>SELECT account_id FROM accounts WHERE account_id IN (?)
     *       AND user_id=? AND status='ACTIVE'.</li>
     *   <li>Diff against input ids to build valid / invalid lists.</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public ValidateAccountsResponse validateAccounts(String userId, Collection<UUID> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return ValidateAccountsResponse.builder()
                    .valid(List.of())
                    .invalid(List.of())
                    .build();
        }
        // Dedupe input while preserving stable order.
        Set<UUID> distinctRequested = new HashSet<>(accountIds);
        Set<UUID> found = accountRepository
                .findByAccountIdInAndUserIdAndStatus(distinctRequested, userId, AccountStatus.ACTIVE)
                .stream()
                .map(Account::getAccountId)
                .collect(Collectors.toSet());

        List<UUID> valid = distinctRequested.stream().filter(found::contains).toList();
        List<UUID> invalid = distinctRequested.stream().filter(id -> !found.contains(id)).toList();
        return ValidateAccountsResponse.builder().valid(valid).invalid(invalid).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Account loadOwned(String userId, UUID accountId) {
        return accountRepository.findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account " + accountId + " not found for user " + userId));
    }

    private static String normaliseCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase();
    }

    /** Throws {@link InvalidCurrencyException} if the code is not ISO 4217. */
    private static void assertValidIsoCurrency(String currency) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidCurrencyException(currency);
        }
    }
}
