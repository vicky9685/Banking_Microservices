package com.bank.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Strategy interface for account number generation.
 * Default implementation generates IBAN-ish 16-digit numbers; swap for branch-coded versions if needed.
 */
public interface AccountNumberGenerator {
    String generate();

    @Component
    class Default implements AccountNumberGenerator {
        private static final SecureRandom RNG = new SecureRandom();

        @Override
        public String generate() {
            StringBuilder sb = new StringBuilder("BNK");
            for (int i = 0; i < 13; i++) sb.append(RNG.nextInt(10));
            return sb.toString();
        }
    }
}
