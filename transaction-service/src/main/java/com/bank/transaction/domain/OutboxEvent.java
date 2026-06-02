package com.bank.transaction.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox pattern.
 * Writing the domain change + outbox row in one DB transaction guarantees
 * at-least-once event publication even if Kafka is briefly unavailable.
 * A scheduled poller drains the outbox and publishes to Kafka.
 */
@Entity
@Table(name = "outbox", indexes = @Index(name = "idx_outbox_processed", columnList = "processed"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String aggregateId;
    @Column(nullable = false)
    private String eventType;
    @Lob @Column(nullable = false)
    private String payload;
    @Column(nullable = false)
    private boolean processed;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant processedAt;
    private int attempts;
}
