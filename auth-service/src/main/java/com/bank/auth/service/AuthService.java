package com.bank.auth.service;

import com.bank.auth.domain.AppUser;
import com.bank.auth.domain.RefreshToken;
import com.bank.auth.dto.AuthDtos.*;
import com.bank.auth.repository.AppUserRepository;
import com.bank.auth.repository.RefreshTokenRepository;
import com.bank.auth.security.LoginAttemptService;
import com.bank.auth.security.PasswordPolicy;
import com.bank.common.exception.BusinessException;
import com.bank.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository repository;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;
    private final PasswordPolicy passwordPolicy;
    private final LoginAttemptService attempts;

    @Value("${app.security.jwt.expiration-ms:900000}") long accessTokenTtl;
    @Value("${app.security.jwt.refresh-expiration-ms:86400000}") long refreshTtl;

    @Transactional
    public void register(RegisterRequest req) {
        passwordPolicy.validate(req.password());
        if (repository.existsByUsername(req.username())) {
            throw new BusinessException("USERNAME_TAKEN", "Username already in use");
        }
        AppUser user = AppUser.builder()
                .username(req.username())
                .passwordHash(encoder.encode(req.password()))
                .email(req.email())
                .customerId(req.customerId())
                .roles(Set.of("CUSTOMER"))
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        repository.save(user);
        log.info("User registered: {}", user.getUsername());
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        attempts.checkNotLocked(req.username());

        AppUser user = repository.findByUsername(req.username())
                .orElseThrow(() -> {
                    attempts.recordFailure(req.username());
                    return new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
                });

        if (!user.isEnabled() || !encoder.matches(req.password(), user.getPasswordHash())) {
            attempts.recordFailure(req.username());
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }
        attempts.recordSuccess(req.username());

        return issueTokens(user);
    }

    /**
     * Rotating refresh: previous token is invalidated; presenting a used token
     * is a token-theft signal and revokes the whole family for the user.
     */
    @Transactional
    public TokenResponse refresh(String presented) {
        String hash = hash(presented);
        RefreshToken token = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Unknown refresh token"));

        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}. Revoking all sessions.", token.getUserId());
            refreshRepo.deleteByUserId(token.getUserId());
            throw new BusinessException("REUSED_REFRESH_TOKEN",
                    "Refresh token reuse detected; all sessions revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("EXPIRED_REFRESH_TOKEN", "Refresh token expired");
        }

        AppUser user = repository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("USER_GONE", "User no longer exists"));

        TokenResponse next = issueTokens(user);
        token.setRevoked(true);
        // Rotation chain breadcrumb (replacedById is set after we persist the new token id below)
        refreshRepo.save(token);
        return next;
    }

    private TokenResponse issueTokens(AppUser user) {
        Map<String, Object> extra = user.getCustomerId() == null
                ? Map.of() : Map.of("customerId", user.getCustomerId().toString());
        String access = jwt.generate(user.getId().toString(), user.getRoles().stream().toList(), extra);

        String refreshPlain = newRefreshTokenString();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(hash(refreshPlain))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMillis(refreshTtl)))
                .revoked(false)
                .build();
        refreshRepo.save(stored);

        return new TokenResponse(access, refreshPlain, accessTokenTtl / 1000, "Bearer");
    }

    private static String newRefreshTokenString() {
        byte[] buf = new byte[48];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String hash(String token) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
