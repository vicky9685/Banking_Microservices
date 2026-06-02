package com.bank.transaction.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transfer = saga instance. State machine drives the orchestrator.
 *
 * State transitions:
 *   INITIATED -> DEBITED -> CREDITED -> COMPLETED
 *   INITIATED -> FAILED
 *   DEBITED   -> COMPENSATING -> FAILED   (credit step failed)
 */
@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_status", columnList = "status"),
        @Index(name = "idx_transfer_from", columnList = "from_account_id"),
        @Index(name = "idx_transfer_to", columnList = "to_account_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(length = 100)
    private String idempotencyKey;

    @Version
    private Long version;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
