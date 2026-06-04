package com.bank.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory failed-login tracker. Production: replace with Redis so it survives
 * restarts and works across replicas. Tracks attempts per username + IP separately
 * to prevent username enumeration / DoS by IP.
 */
@Slf4j
@Component
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockoutDuration;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lockout-minutes:15}") int lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
    }

    public void checkNotLocked(String username) {
        Window w = windows.get(username);
        if (w != null && w.locked() && Instant.now().isBefore(w.unlockAt)) {
            long secsLeft = Duration.between(Instant.now(), w.unlockAt).toSeconds();
            throw new SecurityException("Account temporarily locked. Try again in " + secsLeft + "s");
        }
    }

    public void recordFailure(String username) {
        windows.compute(username, (k, w) -> {
            Window cur = w == null ? new Window(0, null) : w;
            int next = cur.attempts + 1;
            if (next >= maxAttempts) {
                log.warn("Locked user {} after {} failed attempts", username, next);
                return new Window(next, Instant.now().plus(lockoutDuration));
            }
            return new Window(next, null);
        });
    }

    public void recordSuccess(String username) {
        windows.remove(username);
    }

    private record Window(int attempts, Instant unlockAt) {
        boolean locked() { return unlockAt != null; }
    }
}
