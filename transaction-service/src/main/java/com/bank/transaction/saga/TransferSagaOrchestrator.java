package com.bank.transaction.saga;

import com.bank.common.events.KafkaTopics;
import com.bank.common.events.TransactionEvents.*;
import com.bank.transaction.domain.OutboxEvent;
import com.bank.transaction.domain.Transfer;
import com.bank.transaction.domain.TransferStatus;
import com.bank.transaction.repository.OutboxRepository;
import com.bank.transaction.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga orchestrator — listens to TRANSFER_EVENTS and drives state transitions.
 *
 *   AccountDebited  -> issue credit command, status DEBITED
 *   AccountCredited -> mark COMPLETED
 *   TransferFailed  -> mark FAILED; if already DEBITED, dispatch compensation
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransferSagaOrchestrator {

    public static final String CREDIT_COMMANDS = "banking.transfer.credit.commands";

    private final TransferRepository transferRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.TRANSFER_EVENTS, groupId = "transaction-orchestrator")
    @Transactional
    public void onEvent(Object event) {
        switch (event) {
            case AccountDebited e -> handleDebited(e);
            case AccountCredited e -> handleCredited(e);
            case TransferFailed e -> handleFailed(e);
            default -> log.debug("Unhandled event: {}", event.getClass());
        }
    }

    private void handleDebited(AccountDebited e) {
        Transfer t = transferRepository.findById(e.transferId()).orElseThrow();
        if (t.getStatus() != TransferStatus.INITIATED) return;
        t.setStatus(TransferStatus.DEBITED);
        transferRepository.save(t);

        AccountCredited creditCmd = new AccountCredited(
                UUID.randomUUID(), Instant.now(), t.getId().toString(),
                t.getId(), t.getToAccountId(), t.getAmount());
        appendOutbox(CREDIT_COMMANDS, t.getId().toString(), "CreditCommand", creditCmd);
        log.info("Transfer {} debited, issuing credit", t.getId());
    }

    private void handleCredited(AccountCredited e) {
        Transfer t = transferRepository.findById(e.transferId()).orElseThrow();
        if (t.getStatus() == TransferStatus.COMPLETED) return;
        t.setStatus(TransferStatus.COMPLETED);
        transferRepository.save(t);

        TransferCompleted done = new TransferCompleted(
                UUID.randomUUID(), Instant.now(), t.getId().toString(), t.getId());
        appendOutbox(KafkaTopics.NOTIFICATIONS, t.getId().toString(), "TransferCompleted", done);
        log.info("Transfer {} COMPLETED", t.getId());
    }

    private void handleFailed(TransferFailed e) {
        Transfer t = transferRepository.findById(e.transferId()).orElseThrow();
        if (t.getStatus() == TransferStatus.DEBITED) {
            // Credit step failed after debit succeeded → compensate the source.
            t.setStatus(TransferStatus.COMPENSATING);
            t.setFailureReason(e.reason());
            transferRepository.save(t);

            CompensationRequested comp = new CompensationRequested(
                    UUID.randomUUID(), Instant.now(), t.getId().toString(),
                    t.getId(), t.getFromAccountId(), t.getAmount(), e.reason());
            appendOutbox(KafkaTopics.TRANSFER_COMMANDS, t.getId().toString(),
                    "CompensationRequested", comp);
            log.warn("Transfer {} compensating: {}", t.getId(), e.reason());
        } else {
            t.setStatus(TransferStatus.FAILED);
            t.setFailureReason(e.reason());
            transferRepository.save(t);
            log.warn("Transfer {} FAILED at {}: {}", t.getId(), t.getStatus(), e.reason());
        }
    }

    private void appendOutbox(String topic, String aggregateId, String type, Object payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .topic(topic)
                    .aggregateId(aggregateId)
                    .eventType(type)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
