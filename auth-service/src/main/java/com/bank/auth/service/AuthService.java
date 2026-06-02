package com.bank.auth.service;

import com.bank.auth.domain.AppUser;
import com.bank.auth.dto.AuthDtos.*;
import com.bank.auth.repository.AppUserRepository;
import com.bank.common.exception.BusinessException;
import com.bank.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;

    @Transactional
    public void register(RegisterRequest req) {
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
    }

    public TokenResponse login(LoginRequest req) {
        AppUser user = repository.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid username or password"));
        if (!user.isEnabled() || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }
        Map<String, Object> extra = user.getCustomerId() == null
                ? Map.of() : Map.of("customerId", user.getCustomerId().toString());
        String token = jwt.generate(user.getId().toString(), user.getRoles().stream().toList(), extra);
        return new TokenResponse(token, 3600, "Bearer");
    }
}
