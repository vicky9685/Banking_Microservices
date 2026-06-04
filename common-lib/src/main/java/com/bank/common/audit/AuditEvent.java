package com.bank.common.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit row. Every state-changing API call writes one of these.
 *
 * Tamper-evidence: each row stores SHA-256(prev.hash || canonical(this row without hash)).
 * Auditors can verify the chain by walking rows in order and re-hashing.
 * Breaking the chain (mutating a past row) is detectable on the next verification.
 */
@Entity
@Table(name = "audit_log",
       indexes = {
         @Index(name = "idx_audit_actor", columnList = "actor_id"),
         @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
         @Index(name = "idx_audit_at", columnList = "at")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {

    @Id @GeneratedValue
    private UUID id;

    /** Monotonically increasing sequence enforced by the writer. */
    @Column(nullable = false, unique = true)
    private Long sequence;

    @Column(nullable = false)
    private Instant at;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_roles", length = 500)
    private String actorRoles;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(nullable = false, length = 80)
    private String action;          // e.g. "ACCOUNT_DEBITED", "CUSTOMER_KYC_VERIFIED"

    @Column(name = "resource_type", length = 60)
    private String resourceType;
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Lob @Column(length = 4000)
    private String details;         // JSON, no PII

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "row_hash", nullable = false, length = 64)
    private String rowHash;

    @Column(nullable = false)
    private String serviceName;
}
