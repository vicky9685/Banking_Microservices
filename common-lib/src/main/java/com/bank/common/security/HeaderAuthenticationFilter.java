package com.bank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Promotes gateway-injected X-User-Id / X-User-Roles to Spring Security context
 * so @PreAuthorize and method security work in every backend.
 *
 * Runs after the HMAC verifier so unsigned calls never reach this filter.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String userId = req.getHeader("X-User-Id");
        String roles  = req.getHeader("X-User-Roles");
        String customer = req.getHeader("X-Customer-Id");

        if (userId != null && !userId.isBlank()) {
            List<String> roleList = roles == null || roles.isBlank()
                    ? List.of()
                    : Arrays.stream(roles.split(",")).map(String::trim).toList();
            UUID custUuid = parseUuid(customer);
            BankingPrincipal principal = new BankingPrincipal(userId, custUuid, roleList);

            var authorities = roleList.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
