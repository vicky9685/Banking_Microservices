package com.bank.account.security;

import com.bank.account.repository.AccountRepository;
import com.bank.common.security.BankingPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resource-ownership SpEL helper used by @PreAuthorize.
 * Verifies an account belongs to the calling customer before allowing access.
 */
@Component("accountAuthz")
@RequiredArgsConstructor
public class AccountAuthorization {

    private final AccountRepository repository;

    public boolean canRead(Object principal, UUID accountId) {
        if (!(principal instanceof BankingPrincipal p) || p.customerId() == null) return false;
        return repository.findById(accountId)
                .map(a -> p.customerId().equals(a.getCustomerId()))
                .orElse(false);
    }
}
