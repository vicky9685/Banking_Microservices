package com.bank.account.scheduler;

import com.bank.account.domain.AccountStatus;
import com.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Flags accounts inactive for > N months as DORMANT.
 * Real banks need this for regulatory unclaimed-property reporting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DormantAccountJob {

    private final AccountRepository repository;

    @Value("${app.account.dormant-months:6}")
    private int dormantMonths;

    @Scheduled(cron = "${app.account.dormant-cron:0 30 2 * * *}")
    @Transactional
    public void markDormant() {
        Instant cutoff = Instant.now().minus(dormantMonths * 30L, ChronoUnit.DAYS);
        int flipped = 0;
        for (var a : repository.findAll()) {
            if (a.getStatus() == AccountStatus.ACTIVE
                    && a.getUpdatedAt() != null
                    && a.getUpdatedAt().isBefore(cutoff)) {
                a.setStatus(AccountStatus.DORMANT);
                repository.save(a);
                flipped++;
            }
        }
        if (flipped > 0) log.info("Dormancy job moved {} accounts to DORMANT", flipped);
    }
}
