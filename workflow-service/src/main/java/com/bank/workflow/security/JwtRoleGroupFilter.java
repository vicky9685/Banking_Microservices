package com.bank.workflow.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Bridges the gateway-issued JWT to the Camunda IdentityService for the request.
 *
 * The API Gateway already validates the JWT and injects:
 *   X-User-Id     — UUID of the user
 *   X-User-Roles  — comma-separated roles (e.g. "RISK_OFFICER,CUSTOMER")
 *
 * We map roles → BPMN candidate groups so a user task with
 * candidateGroups="risk-officers" only shows up for users whose X-User-Roles
 * contains RISK_OFFICER.
 *
 * Camunda authorization (camunda.bpm.authorization.enabled=true) then enforces
 * actual access in the engine.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtRoleGroupFilter {

    private final IdentityService identityService;

    /** Role → group mapping. Keep in sync with CamundaIdentityBootstrap. */
    private static String roleToGroup(String role) {
        return switch (role) {
            case "CUSTOMER"             -> "customers";
            case "RISK_OFFICER"         -> "risk-officers";
            case "SENIOR_RISK_OFFICER"  -> "senior-risk-officers";
            case "UNDERWRITER"          -> "underwriters";
            case "DISPUTE_ANALYST"      -> "dispute-analysts";
            case "LEGAL"                -> "legal";
            default                     -> null;
        };
    }

    @Bean
    public FilterRegistrationBean<Filter> identityBridge() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setName("camunda-identity-bridge");
        reg.setOrder(10);
        reg.addUrlPatterns("/api/workflows/*", "/engine-rest/*");
        reg.setFilter((req, res, chain) -> doFilter(req, res, chain));
        return reg;
    }

    private void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (req instanceof HttpServletRequest http) {
                String userId = http.getHeader("X-User-Id");
                String rolesHeader = http.getHeader("X-User-Roles");
                if (userId != null && !userId.isBlank()) {
                    List<String> groups = rolesHeader == null
                            ? List.of()
                            : Arrays.stream(rolesHeader.split(","))
                                .map(String::trim)
                                .map(JwtRoleGroupFilter::roleToGroup)
                                .filter(g -> g != null)
                                .toList();
                    identityService.setAuthentication(userId, groups);
                }
            }
            chain.doFilter(req, res);
        } finally {
            identityService.clearAuthentication();
        }
    }
}
