package com.bank.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security filter chain for back-end services.
 * - No sessions (JWT is validated at the gateway).
 * - CSRF disabled because we have no cookies (Bearer only).
 * - Actuator health/info are public; everything else requires authentication
 *   (i.e. the HeaderAuthenticationFilter must have populated the context).
 *
 * Method security is enabled so controllers/services can use @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
@ConditionalOnClass(HttpSecurity.class)
public class BankingSecurityConfig {

    private final String sharedKey;
    private final boolean serviceAuthEnabled;

    public BankingSecurityConfig(
            @Value("${app.security.service-auth.hmac-key:${service.hmac.key:dev-only-internal-shared-key-please-rotate}}") String sharedKey,
            @Value("${app.security.service-auth.enabled:true}") boolean serviceAuthEnabled) {
        this.sharedKey = sharedKey;
        this.serviceAuthEnabled = serviceAuthEnabled;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain bankingFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        if (serviceAuthEnabled) {
            http.addFilterBefore(new ServiceSignatureVerificationFilter(sharedKey), UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
