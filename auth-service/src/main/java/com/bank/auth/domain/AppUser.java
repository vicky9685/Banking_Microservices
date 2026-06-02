package com.bank.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_user_username", columnList = "username", unique = true))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String email;

    private UUID customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
