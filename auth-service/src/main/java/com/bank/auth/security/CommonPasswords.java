package com.bank.auth.security;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Tiny embedded denylist for common leaked passwords.
 * Production: swap with HIBP k-anonymity API check.
 */
final class CommonPasswords {
    private CommonPasswords() {}

    private static final Set<String> COMMON = Set.of(
            "password123!", "qwerty12345!", "letmein2024!", "admin1234!",
            "welcome2024!", "p@ssw0rd1234", "passw0rd!2024"
    );

    static final Predicate<String> IS_COMMON = pw -> {
        if (COMMON.contains(pw)) return true;
        // very simple heuristic: starts with "password" or "qwerty"
        return pw.startsWith("password") || pw.startsWith("qwerty");
    };
}
