package com.personal.finance.account.repository;

import com.personal.finance.account.entity.Account;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account persistence boundary. Every finder is scoped by {@code userId} so
 * cross-user access is impossible by construction — spec §3.1: "do not reveal
 * whether the account exists for another user".
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /** Spec §3.1 GET /v1/accounts/{id} — single account by id, scoped to user. */
    Optional<Account> findByAccountIdAndUserId(UUID accountId, String userId);

    /** Spec §3.1 GET /v1/accounts — list by user + status filter. */
    List<Account> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, AccountStatus status);

    /** Spec §3.1 GET /v1/accounts — list by user + status + type filter. */
    List<Account> findByUserIdAndStatusAndAccountTypeOrderByCreatedAtDesc(
            String userId, AccountStatus status, AccountType accountType);

    /**
     * Spec §3.1 GET /v1/accounts/validate — batch lookup constrained to ACTIVE
     * accounts owned by {@code userId}. The caller diffs against the input
     * list to produce valid / invalid sets.
     */
    List<Account> findByAccountIdInAndUserIdAndStatus(
            Collection<UUID> accountIds, String userId, AccountStatus status);
}
