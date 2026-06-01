package com.personal.finance.account.entity;

import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Accounts table — spec §1.2. One row per bank account per user. Logical FK
 * target for transactions in the Transaction Service database.
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_accounts_account_type", columnList = "account_type"),
        @Index(name = "idx_accounts_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @UuidGenerator
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    /** Card number, IBAN, or any reference — spec §1.2 note: no unique constraint. */
    @Column(name = "account_code", columnDefinition = "TEXT")
    private String accountCode;

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** ISO 4217 currency code. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "opening_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
