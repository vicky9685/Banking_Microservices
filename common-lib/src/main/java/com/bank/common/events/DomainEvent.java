package com.bank.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events traversing Kafka.
 * Polymorphic serialization via @class to support a single event topic if desired.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransactionEvents.TransferInitiated.class),
        @JsonSubTypes.Type(value = TransactionEvents.AccountDebited.class),
        @JsonSubTypes.Type(value = TransactionEvents.AccountCredited.class),
        @JsonSubTypes.Type(value = TransactionEvents.TransferCompleted.class),
        @JsonSubTypes.Type(value = TransactionEvents.TransferFailed.class),
        @JsonSubTypes.Type(value = TransactionEvents.CompensationRequested.class)
})
public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String aggregateId();
}
