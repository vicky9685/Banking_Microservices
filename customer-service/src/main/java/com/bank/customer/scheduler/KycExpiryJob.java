package com.bank.customer.scheduler;

import com.bank.customer.domain.KycStatus;
import com.bank.customer.repository.CustomerRepository;
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
 * Marks customer KYC as EXPIRED after the configured retention window.
 * Compliance requires KYC re-verification at least every 365 days.
 *
 * Runs daily at 02:00. In multi-replica deployments, swap to ShedLock
 * (or wrap with a Postgres advisory lock) so only one replica fires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class KycExpiryJob {

    private final CustomerRepository repository;

    @Value("${app.kyc.expiry-days:365}")
    private int expiryDays;

    @Scheduled(cron = "${app.kyc.cron:0 0 2 * * *}")
    @Transactional
    public void expireStaleKyc() {
        Instant cutoff = Instant.now().minus(expiryDays, ChronoUnit.DAYS);
        int updated = 0;
        for (var c : repository.findAll()) {
            if (c.getKycStatus() == KycStatus.VERIFIED
                    && c.getUpdatedAt() != null
                    && c.getUpdatedAt().isBefore(cutoff)) {
                c.setKycStatus(KycStatus.EXPIRED);
                repository.save(c);
                updated++;
            }
        }
        if (updated > 0) log.info("KYC expiry job moved {} customers to EXPIRED", updated);
    }
}
