package com.personal.finance.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Account Service entry point — spec §3.1. Owns {@code account_db}; serves
 * account CRUD plus the internal {@code /v1/accounts/validate} endpoint that
 * Bulk Upload Service uses to verify ownership.
 *
 * <p>The shared {@code ApiResponseBodyAdvice} and {@code GlobalExceptionHandler}
 * are picked up via {@code CommonWebAutoConfiguration} in finance-common.
 * Spring Security (transitively on the classpath via finance-common) is
 * gated by {@code AccountSecurityConfig} which permits the service's paths.
 */
@SpringBootApplication
public class FinanceAccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceAccountServiceApplication.class, args);
    }
}
