package com.bank.transaction.scheduler;

import com.bank.transaction.domain.TransferStatus;
import com.bank.transaction.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/**
 * Detects sagas stuck in intermediate states (INITIATED/DEBITED) past a SLA.
 * Stuck sagas are alerted; production would auto-compensate or page on-call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class StuckSagaJob {

    private static final EnumSet<TransferStatus> INTERMEDIATE =
            EnumSet.of(TransferStatus.INITIATED, TransferStatus.DEBITED);

    private final TransferRepository repository;

    @Value("${app.saga.stuck-timeout-seconds:300}")
    private long stuckTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.saga.detector-interval-ms:60000}")
    @Transactional(readOnly = true)
    public void detectStuck() {
        Instant cutoff = Instant.now().minus(stuckTimeoutSeconds, ChronoUnit.SECONDS);
        long stuckCount = repository.findAll().stream()
                .filter(t -> INTERMEDIATE.contains(t.getStatus()))
                .filter(t -> t.getUpdatedAt() != null && t.getUpdatedAt().isBefore(cutoff))
                .peek(t -> log.warn("Stuck transfer detected id={} status={} age={}s",
                        t.getId(), t.getStatus(),
                        ChronoUnit.SECONDS.between(t.getUpdatedAt(), Instant.now())))
                .count();
        if (stuckCount > 0) {
            log.warn("StuckSagaJob: {} transfers exceed SLA of {}s", stuckCount, stuckTimeoutSeconds);
        }
    }
}
