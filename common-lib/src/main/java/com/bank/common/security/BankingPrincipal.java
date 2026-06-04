package com.bank.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated identity for the current request, derived from gateway-injected headers.
 * Available via SecurityContextHolder.getContext().getAuthentication().getPrincipal().
 */
public record BankingPrincipal(String userId, UUID customerId, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean ownsCustomer(UUID id) {
        return customerId != null && customerId.equals(id);
    }
}
