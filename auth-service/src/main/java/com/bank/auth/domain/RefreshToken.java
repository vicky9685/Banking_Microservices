package com.bank.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side refresh token. Stored hashed so DB compromise doesn't yield live tokens.
 * Single-use: each refresh rotates the token (chained refresh-token rotation).
 */
@Entity
@Table(name = "refresh_tokens",
       indexes = {
         @Index(name = "idx_refresh_user", columnList = "user_id"),
         @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true)
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /** True once the refresh has been used or explicitly revoked. */
    @Column(nullable = false)
    private boolean revoked;

    /** When revoked, the id of the token that replaced it (so we can detect token reuse attacks). */
    private UUID replacedById;
}
