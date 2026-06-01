package com.personal.finance.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.request.UpdateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.dto.response.ValidateAccountsResponse;
import com.personal.finance.account.enums.AccountStatus;
import com.personal.finance.account.enums.AccountType;
import com.personal.finance.account.exception.AccountNotFoundException;
import com.personal.finance.account.exception.InvalidCurrencyException;
import com.personal.finance.account.service.AccountService;
import com.personal.finance.common.web.ApiResponseBodyAdvice;
import com.personal.finance.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class AccountControllerTest {

    private static final String USER_ID = "user-123";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AccountService accountService;

    // ── POST /v1/accounts ────────────────────────────────────────────────

    @Test
    void createAccount_givenValidRequest_thenReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.createAccount(eq(USER_ID), any())).thenReturn(sampleResponse(id));

        mvc.perform(post("/v1/accounts")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest("USD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(id.toString()))
                .andExpect(jsonPath("$.data.accountName").value("Main Checking"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createAccount_givenMissingBankName_thenReturns400() throws Exception {
        CreateAccountRequest req = createRequest("USD");
        req.setBankName(null);

        mvc.perform(post("/v1/accounts")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void createAccount_givenMissingUserId_thenReturns400() throws Exception {
        // No X-User-Id header — MissingRequestHeaderException → 400 via common.
        mvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest("USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void createAccount_givenInvalidCurrency_thenReturns400() throws Exception {
        when(accountService.createAccount(eq(USER_ID), any()))
                .thenThrow(new InvalidCurrencyException("ZZZ"));

        mvc.perform(post("/v1/accounts")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest("ZZZ"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENCY"));
    }

    // ── GET /v1/accounts ─────────────────────────────────────────────────

    @Test
    void listAccounts_givenNoFilters_thenReturns200_andDefaultsToActive() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.listAccounts(USER_ID, null, null)).thenReturn(List.of(sampleResponse(id)));

        mvc.perform(get("/v1/accounts").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].accountId").value(id.toString()));
    }

    @Test
    void listAccounts_givenStatusAndTypeFilter_thenForwardsToService() throws Exception {
        when(accountService.listAccounts(USER_ID, AccountStatus.CLOSED, AccountType.LIABILITY))
                .thenReturn(List.of());

        mvc.perform(get("/v1/accounts")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("status", "CLOSED")
                        .param("type", "LIABILITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listAccounts_givenMissingUserId_thenReturns400() throws Exception {
        mvc.perform(get("/v1/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    // ── GET /v1/accounts/{id} ────────────────────────────────────────────

    @Test
    void getAccount_givenExistingId_thenReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(USER_ID, id)).thenReturn(sampleResponse(id));

        mvc.perform(get("/v1/accounts/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(id.toString()));
    }

    @Test
    void getAccount_givenNonExistentId_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(USER_ID, id))
                .thenThrow(new AccountNotFoundException("Account " + id + " not found"));

        mvc.perform(get("/v1/accounts/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void getAccount_givenMissingUserId_thenReturns400() throws Exception {
        mvc.perform(get("/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /v1/accounts/{id} ────────────────────────────────────────────

    @Test
    void updateAccount_givenValidPartialRequest_thenReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.updateAccount(eq(USER_ID), eq(id), any())).thenReturn(sampleResponse(id));
        UpdateAccountRequest req = new UpdateAccountRequest();
        req.setBankName("New Bank");

        mvc.perform(put("/v1/accounts/{id}", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(id.toString()));
    }

    @Test
    void updateAccount_givenNonExistentId_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.updateAccount(eq(USER_ID), eq(id), any()))
                .thenThrow(new AccountNotFoundException("Account " + id + " not found"));

        mvc.perform(put("/v1/accounts/{id}", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }

    // ── DELETE /v1/accounts/{id} ─────────────────────────────────────────

    @Test
    void closeAccount_givenExistingId_thenReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/v1/accounts/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeAccount_givenNonExistentId_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new AccountNotFoundException("Account " + id + " not found"))
                .when(accountService).closeAccount(USER_ID, id);

        mvc.perform(delete("/v1/accounts/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }

    // ── GET /v1/accounts/validate ────────────────────────────────────────

    @Test
    void validateAccounts_givenMixedIds_thenReturns200WithSplit() throws Exception {
        UUID good = UUID.randomUUID();
        UUID bad = UUID.randomUUID();
        when(accountService.validateAccounts(eq(USER_ID), any()))
                .thenReturn(ValidateAccountsResponse.builder()
                        .valid(List.of(good))
                        .invalid(List.of(bad))
                        .build());

        mvc.perform(get("/v1/accounts/validate")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("ids", good.toString() + "," + bad.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid[0]").value(good.toString()))
                .andExpect(jsonPath("$.data.invalid[0]").value(bad.toString()));
    }

    @Test
    void validateAccounts_givenMissingIdsParam_thenReturns400() throws Exception {
        mvc.perform(get("/v1/accounts/validate").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CreateAccountRequest createRequest(String currency) {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountName("Main Checking");
        req.setBankName("DBS Bank");
        req.setBankCode("DBSSSGSG");
        req.setAccountType(AccountType.ASSET);
        req.setCurrency(currency);
        req.setOpeningBalance(new BigDecimal("1000.0000"));
        return req;
    }

    private AccountResponse sampleResponse(UUID id) {
        return AccountResponse.builder()
                .accountId(id)
                .accountName("Main Checking")
                .bankName("DBS Bank")
                .accountType(AccountType.ASSET)
                .currency("USD")
                .openingBalance(new BigDecimal("1000.0000"))
                .status(AccountStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
