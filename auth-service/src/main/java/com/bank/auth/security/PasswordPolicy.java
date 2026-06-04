package com.bank.auth.security;

import com.bank.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * NIST-aligned password policy: length-first with character variety as a backstop.
 *  - min 12 chars (banking sector guidance > NIST 800-63 baseline)
 *  - at least 1 upper, 1 lower, 1 digit, 1 symbol
 *  - rejects common pwned patterns
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LEN = 12;
    private static final Pattern UPPER  = Pattern.compile("[A-Z]");
    private static final Pattern LOWER  = Pattern.compile("[a-z]");
    private static final Pattern DIGIT  = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String password) {
        if (password == null || password.length() < MIN_LEN) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Password must be at least " + MIN_LEN + " characters");
        }
        if (!UPPER.matcher(password).find()
                || !LOWER.matcher(password).find()
                || !DIGIT.matcher(password).find()
                || !SYMBOL.matcher(password).find()) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Password must include upper, lower, digit, and symbol characters");
        }
        if (CommonPasswords.IS_COMMON.test(password.toLowerCase())) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Password matches a commonly leaked password");
        }
    }
}
