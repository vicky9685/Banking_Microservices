package com.bank.transaction.service;

import com.bank.common.events.KafkaTopics;
import com.bank.common.events.TransactionEvents.TransferInitiated;
import com.bank.common.exception.ResourceNotFoundException;
import com.bank.transaction.domain.OutboxEvent;
import com.bank.transaction.domain.Transfer;
import com.bank.transaction.domain.TransferStatus;
import com.bank.transaction.dto.TransferDtos.*;
import com.bank.common.events.FraudAlert;
import com.bank.transaction.fraud.FraudAlertPublisher;
import com.bank.transaction.fraud.FraudDetector;
import com.bank.transaction.repository.OutboxRepository;
import com.bank.transaction.repository.TransferRepository;
import com.bank.transaction.workflow.WorkflowClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga orchestrator entry point.
 *
 * Why orchestration over choreography here:
 * Transfers have explicit state and need targeted compensation. A central
 * orchestrator gives us a single audit trail and clear failure handling
 * without spreading saga logic across services.
 *
 * Idempotency: Clients pass an optional idempotency key. Duplicate keys return
 * the existing transfer rather than initiating a new saga.
 *
 * Outbox: Initial event is persisted in the same transaction as the Transfer row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final FraudDetector fraudDetector;
    private final FraudAlertPublisher fraudAlertPublisher;
    private final WorkflowClient workflowClient;

    @Transactional
    public TransferResponse initiate(TransferRequest req) {
        if (req.idempotencyKey() != null) {
            var existing = transferRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) return toResponse(existing.get());
        }

        Transfer transfer = Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccountId(req.fromAccountId())
                .toAccountId(req.toAccountId())
                .amount(req.amount())
                .currency(req.currency())
                .status(TransferStatus.INITIATED)
                .idempotencyKey(req.idempotencyKey())
                .build();
        transferRepository.save(transfer);

        // Fraud scoring runs synchronously via Drools, alerts are pushed over Solace
        // (low-latency, guaranteed) so the saga is not blocked by Kafka backpressure.
        var alerts = fraudDetector.evaluate(transfer, req.fromAccountId());
        alerts.forEach(fraudAlertPublisher::publish);

        // If any CRITICAL alert fired, pause the saga and hand off to Camunda for
        // human approval. The saga resumes only after a /decision callback.
        boolean needsApproval = alerts.stream()
                .anyMatch(a -> a.severity() == FraudAlert.Severity.CRITICAL);

        if (needsApproval) {
            transfer.setStatus(TransferStatus.AWAITING_APPROVAL);
            transferRepository.save(transfer);
            workflowClient.startHighValueApproval(new WorkflowClient.HighValueRequest(
                    transfer.getId(), req.fromAccountId(),
                    transfer.getAmount(), transfer.getCurrency()));
            log.info("Transfer {} held for human approval", transfer.getId());
            return toResponse(transfer);
        }

        publishInitiated(transfer);
        log.info("Transfer saga initiated id={} amount={}", transfer.getId(), transfer.getAmount());
        return toResponse(transfer);
    }

    /**
     * Callback invoked by the workflow-service when a human reaches a decision.
     */
    @Transactional
    public TransferResponse applyDecision(UUID transferId, boolean approved, String comment) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));
        if (transfer.getStatus() != TransferStatus.AWAITING_APPROVAL) {
            log.warn("Decision callback for transfer {} ignored, current status={}",
                    transferId, transfer.getStatus());
            return toResponse(transfer);
        }
        if (!approved) {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailureReason("Rejected by approver: " + comment);
            transferRepository.save(transfer);
            return toResponse(transfer);
        }
        transfer.setStatus(TransferStatus.APPROVED);
        transferRepository.save(transfer);
        publishInitiated(transfer);
        log.info("Transfer {} approved by workflow, saga resumed", transferId);
        return toResponse(transfer);
    }

    private void publishInitiated(Transfer transfer) {
        TransferInitiated event = new TransferInitiated(
                UUID.randomUUID(), Instant.now(), transfer.getId().toString(),
                transfer.getId(), transfer.getFromAccountId(), transfer.getToAccountId(),
                transfer.getAmount(), transfer.getCurrency());
        appendOutbox(KafkaTopics.TRANSFER_COMMANDS, transfer.getId().toString(),
                "TransferInitiated", event);
    }

    @Cacheable(cacheNames = "transfers:by-id", key = "#id")
    @Transactional(readOnly = true)
    public TransferResponse get(UUID id) {
        return transferRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }

    private TransferResponse toResponse(Transfer t) {
        return new TransferResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getCurrency(), t.getStatus(), t.getFailureReason(), t.getCreatedAt());
    }
}
