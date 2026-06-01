package com.personal.finance.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Account Service security chain. Runs at {@link Order @Order(0)} so it wins
 * against finance-common's catch-all chains and Spring Boot's default
 * {@code SecurityAutoConfiguration}, which would otherwise gate every request
 * with HTTP Basic and produce empty 401 responses.
 *
 * <p>Authorization for this service is purely header-based ({@code X-User-Id});
 * Spring Security is engaged only to disable CSRF, set the session policy to
 * stateless, and permit the spec endpoints. The header itself is read in the
 * controller layer.
 */
@Configuration
@Slf4j
public class AccountSecurityConfig {

    private static final String[] PERMITTED_PATTERNS = {
            "/v1/accounts/**",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean("accountSecurityFilterChain")
    @Order(0)
    public SecurityFilterChain accountSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PERMITTED_PATTERNS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        log.info("Account-service security chain registered — permitting: {}", String.join(", ", PERMITTED_PATTERNS));
        return http.build();
    }
}
