package com.bank.transaction.outbox;

import com.bank.transaction.domain.OutboxEvent;
import com.bank.transaction.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox and ships events to Kafka. At-least-once delivery; consumers must be idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.findUnpublished(PageRequest.of(0, 50));
        if (batch.isEmpty()) return;

        for (OutboxEvent ev : batch) {
            try {
                Object payload = objectMapper.readValue(ev.getPayload(), Object.class);
                kafka.send(ev.getTopic(), ev.getAggregateId(), payload).get();
                ev.setProcessed(true);
                ev.setProcessedAt(Instant.now());
            } catch (Exception e) {
                ev.setAttempts(ev.getAttempts() + 1);
                log.warn("Failed to publish outbox event {} (attempt {})", ev.getId(), ev.getAttempts(), e);
            }
        }
        repository.saveAll(batch);
    }
}
