package com.bank.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Saga event vocabulary for fund transfers.
 * Using sealed records to enforce closed event taxonomy.
 */
public final class TransactionEvents {
    private TransactionEvents() {}

    public record TransferInitiated(UUID eventId, Instant occurredAt, String aggregateId,
                                    UUID transferId, UUID fromAccountId, UUID toAccountId,
                                    BigDecimal amount, String currency) implements DomainEvent {}

    public record AccountDebited(UUID eventId, Instant occurredAt, String aggregateId,
                                 UUID transferId, UUID accountId, BigDecimal amount) implements DomainEvent {}

    public record AccountCredited(UUID eventId, Instant occurredAt, String aggregateId,
                                  UUID transferId, UUID accountId, BigDecimal amount) implements DomainEvent {}

    public record TransferCompleted(UUID eventId, Instant occurredAt, String aggregateId,
                                    UUID transferId) implements DomainEvent {}

    public record TransferFailed(UUID eventId, Instant occurredAt, String aggregateId,
                                 UUID transferId, String reason) implements DomainEvent {}

    public record CompensationRequested(UUID eventId, Instant occurredAt, String aggregateId,
                                        UUID transferId, UUID accountId, BigDecimal amount,
                                        String reason) implements DomainEvent {}
}
