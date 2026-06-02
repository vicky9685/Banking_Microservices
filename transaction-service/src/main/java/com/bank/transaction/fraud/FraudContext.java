package com.bank.transaction.fraud;

import com.bank.common.events.FraudAlert;
import com.bank.common.events.FraudAlert.Severity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fact passed into the Drools session. Rules add alerts; we collect them after evaluation.
 * Keeping the shape simple keeps DRL rules readable.
 */
@Data
public class FraudContext {
    private UUID transferId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private String fromAccountId;
    private String toAccountId;
    private int dailyTransferCount;          // populated by service before drools fires
    private BigDecimal dailyTransferTotal;
    private Instant occurredAt;
    private boolean knownCounterparty;
    private boolean highRiskCountry;

    private final List<FraudAlert> alerts = new ArrayList<>();

    public void raise(Severity severity, String rule, String description) {
        alerts.add(new FraudAlert(
                UUID.randomUUID(), Instant.now(), customerId, transferId,
                amount, currency, severity, rule, description));
    }
}
