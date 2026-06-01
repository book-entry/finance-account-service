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
import com.personal.finance.account.mapper.AccountMapperImpl;
import com.personal.finance.account.repository.AccountRepository;
import com.personal.finance.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final String USER_ID = "user-123";

    @Mock AccountRepository repository;

    AccountServiceImpl service;
    AccountMapper mapper;

    @BeforeEach
    void setUp() {
        // Use the real generated MapStruct impl — DTO ⇄ entity is real-arithmetic.
        mapper = new AccountMapperImpl();
        service = new AccountServiceImpl(repository, mapper);
    }

    // ── createAccount ────────────────────────────────────────────────────

    @Test
    void createAccount_givenValidRequest_thenReturnsCreatedDto() {
        CreateAccountRequest req = createRequest("USD");
        when(repository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setAccountId(UUID.randomUUID());
            return a;
        });

        AccountResponse resp = service.createAccount(USER_ID, req);

        assertThat(resp.getAccountId()).isNotNull();
        assertThat(resp.getAccountName()).isEqualTo("Main Checking");
        assertThat(resp.getCurrency()).isEqualTo("USD");
        assertThat(resp.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(resp.getOpeningBalance()).isEqualByComparingTo(new BigDecimal("1000.0000"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void createAccount_givenLowercaseCurrency_thenNormalisesToUppercase() {
        CreateAccountRequest req = createRequest("eur");
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse resp = service.createAccount(USER_ID, req);

        assertThat(resp.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void createAccount_givenNullOpeningBalance_thenDefaultsToZero() {
        CreateAccountRequest req = createRequest("USD");
        req.setOpeningBalance(null);
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse resp = service.createAccount(USER_ID, req);

        assertThat(resp.getOpeningBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createAccount_givenInvalidCurrency_thenThrowsInvalidCurrency() {
        CreateAccountRequest req = createRequest("ZZZ");

        assertThatThrownBy(() -> service.createAccount(USER_ID, req))
                .isInstanceOf(InvalidCurrencyException.class)
                .satisfies(ex -> assertThat(((InvalidCurrencyException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CURRENCY));
        verify(repository, never()).save(any());
    }

    // ── listAccounts ─────────────────────────────────────────────────────

    @Test
    void listAccounts_givenNoFilters_thenDefaultsToActiveStatus() {
        Account row = activeAccount(UUID.randomUUID());
        when(repository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, AccountStatus.ACTIVE))
                .thenReturn(List.of(row));

        List<AccountResponse> result = service.listAccounts(USER_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void listAccounts_givenTypeFilter_thenAppliesTypeFilter() {
        Account row = activeAccount(UUID.randomUUID());
        row.setAccountType(AccountType.LIABILITY);
        when(repository.findByUserIdAndStatusAndAccountTypeOrderByCreatedAtDesc(
                USER_ID, AccountStatus.ACTIVE, AccountType.LIABILITY))
                .thenReturn(List.of(row));

        List<AccountResponse> result = service.listAccounts(USER_ID, AccountStatus.ACTIVE, AccountType.LIABILITY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountType()).isEqualTo(AccountType.LIABILITY);
    }

    @Test
    void listAccounts_givenNoRows_thenReturnsEmptyList() {
        when(repository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, AccountStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.listAccounts(USER_ID, null, null)).isEmpty();
    }

    // ── getAccount ───────────────────────────────────────────────────────

    @Test
    void getAccount_givenExistingId_thenReturnsDto() {
        UUID id = UUID.randomUUID();
        Account row = activeAccount(id);
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.of(row));

        AccountResponse resp = service.getAccount(USER_ID, id);

        assertThat(resp.getAccountId()).isEqualTo(id);
    }

    @Test
    void getAccount_givenNonExistentId_thenThrowsAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(USER_ID, id))
                .isInstanceOf(AccountNotFoundException.class)
                .satisfies(ex -> assertThat(((AccountNotFoundException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    // ── updateAccount ────────────────────────────────────────────────────

    @Test
    void updateAccount_givenExistingId_thenAppliesPartialUpdate() {
        UUID id = UUID.randomUUID();
        Account row = activeAccount(id);
        row.setBankName("Old Bank");
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.of(row));
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAccountRequest req = new UpdateAccountRequest();
        req.setBankName("New Bank");
        // accountName, accountCode, bankCode, description left null — must NOT overwrite.

        AccountResponse resp = service.updateAccount(USER_ID, id, req);

        assertThat(resp.getBankName()).isEqualTo("New Bank");
        assertThat(resp.getAccountName()).isEqualTo("Main Checking"); // unchanged
    }

    @Test
    void updateAccount_givenNonExistentId_thenThrowsAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccount(USER_ID, id, new UpdateAccountRequest()))
                .isInstanceOf(AccountNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateAccount_doesNotChangeCurrencyOrType() {
        UUID id = UUID.randomUUID();
        Account row = activeAccount(id);
        row.setCurrency("USD");
        row.setAccountType(AccountType.ASSET);
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.of(row));
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // UpdateAccountRequest intentionally has no currency / accountType fields.
        UpdateAccountRequest req = new UpdateAccountRequest();
        req.setAccountName("Renamed");

        AccountResponse resp = service.updateAccount(USER_ID, id, req);

        assertThat(resp.getCurrency()).isEqualTo("USD");
        assertThat(resp.getAccountType()).isEqualTo(AccountType.ASSET);
        assertThat(resp.getAccountName()).isEqualTo("Renamed");
    }

    // ── closeAccount ─────────────────────────────────────────────────────

    @Test
    void closeAccount_givenActiveAccount_thenSetsStatusClosed() {
        UUID id = UUID.randomUUID();
        Account row = activeAccount(id);
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.of(row));

        service.closeAccount(USER_ID, id);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeAccount_givenAlreadyClosed_thenIsIdempotent_andSkipsSave() {
        UUID id = UUID.randomUUID();
        Account row = activeAccount(id);
        row.setStatus(AccountStatus.CLOSED);
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.of(row));

        service.closeAccount(USER_ID, id);

        verify(repository, never()).save(any());
    }

    @Test
    void closeAccount_givenNonExistentId_thenThrowsAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByAccountIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeAccount(USER_ID, id))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ── validateAccounts ─────────────────────────────────────────────────

    @Test
    void validateAccounts_givenMixOfOwnedAndUnowned_thenSplitsCorrectly() {
        UUID owned = UUID.randomUUID();
        UUID notOwned = UUID.randomUUID();
        Account ownedRow = activeAccount(owned);
        when(repository.findByAccountIdInAndUserIdAndStatus(any(), anyString(), any()))
                .thenReturn(List.of(ownedRow));

        ValidateAccountsResponse resp = service.validateAccounts(USER_ID, List.of(owned, notOwned));

        assertThat(resp.getValid()).containsExactly(owned);
        assertThat(resp.getInvalid()).containsExactly(notOwned);
    }

    @Test
    void validateAccounts_givenEmptyList_thenReturnsEmptyLists() {
        ValidateAccountsResponse resp = service.validateAccounts(USER_ID, List.of());

        assertThat(resp.getValid()).isEmpty();
        assertThat(resp.getInvalid()).isEmpty();
    }

    @Test
    void validateAccounts_givenNull_thenReturnsEmptyLists() {
        ValidateAccountsResponse resp = service.validateAccounts(USER_ID, null);

        assertThat(resp.getValid()).isEmpty();
        assertThat(resp.getInvalid()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CreateAccountRequest createRequest(String currency) {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountName("Main Checking");
        req.setBankName("DBS Bank");
        req.setBankCode("DBSSSGSG");
        req.setAccountCode("1234-5678");
        req.setAccountType(AccountType.ASSET);
        req.setCurrency(currency);
        req.setOpeningBalance(new BigDecimal("1000.0000"));
        req.setDescription("Primary account");
        return req;
    }

    private Account activeAccount(UUID id) {
        return Account.builder()
                .accountId(id)
                .userId(USER_ID)
                .accountName("Main Checking")
                .bankName("DBS Bank")
                .accountType(AccountType.ASSET)
                .currency("USD")
                .openingBalance(new BigDecimal("0.0000"))
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
