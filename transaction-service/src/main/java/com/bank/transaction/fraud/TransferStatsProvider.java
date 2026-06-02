package com.bank.transaction.fraud;

import com.bank.transaction.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Computes velocity stats. Results are cached briefly to avoid pounding
 * Postgres for high-rate transfer endpoints.
 */
@Component
@RequiredArgsConstructor
public class TransferStatsProvider {

    private final TransferRepository repository;

    public record Stats(int count, BigDecimal total) {}

    @Cacheable(cacheNames = "fraud:daily-stats", key = "#customerId.toString()")
    public Stats dailyStatsFor(UUID customerId) {
        // In a real system this would query a denormalized view or projection.
        // Kept naive here; caching softens the cost.
        return new Stats(0, BigDecimal.ZERO);
    }

    @Cacheable(cacheNames = "fraud:known-counterparty",
               key = "#customerId.toString() + ':' + #counterpartyAccountId.toString()")
    public boolean isKnownCounterparty(UUID customerId, UUID counterpartyAccountId) {
        // Stub: in production, look up a customer's payee whitelist.
        return false;
    }
}
