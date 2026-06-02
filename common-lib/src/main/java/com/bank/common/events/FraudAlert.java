package com.bank.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fraud / risk alert. Carried on Solace because:
 *  - low latency (push, broker fan-out within ms)
 *  - guaranteed messaging with per-message acks suits high-stakes alerts
 *  - hierarchical topics (banking/fraud/alerts/v1/{severity}/{customerId})
 *    let downstream consumers subscribe by severity or by customer
 *  - request/reply model (not used here, but available for adjudication)
 */
public record FraudAlert(
        UUID alertId,
        Instant occurredAt,
        UUID customerId,
        UUID transferId,
        BigDecimal amount,
        String currency,
        Severity severity,
        String rule,
        String description
) {
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
}
