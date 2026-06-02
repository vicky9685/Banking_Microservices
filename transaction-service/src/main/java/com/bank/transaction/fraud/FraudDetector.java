package com.bank.transaction.fraud;

import com.bank.common.events.FraudAlert;
import com.bank.transaction.domain.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Drools-backed fraud evaluation.
 * Why Drools here: business analysts can edit .drl without code changes,
 * rules are unit-testable in isolation, and salience/agenda groups give us
 * deterministic ordering across many rules.
 *
 * The detector is stateless from the caller's perspective: each evaluation
 * spawns a fresh stateful KieSession (cheap) and disposes it after firing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudDetector {

    private final KieContainer kieContainer;
    private final TransferStatsProvider statsProvider;

    public List<FraudAlert> evaluate(Transfer transfer, UUID customerId) {
        FraudContext ctx = new FraudContext();
        ctx.setTransferId(transfer.getId());
        ctx.setCustomerId(customerId);
        ctx.setAmount(transfer.getAmount());
        ctx.setCurrency(transfer.getCurrency());
        ctx.setFromAccountId(String.valueOf(transfer.getFromAccountId()));
        ctx.setToAccountId(String.valueOf(transfer.getToAccountId()));
        ctx.setOccurredAt(Instant.now());

        TransferStatsProvider.Stats stats = statsProvider.dailyStatsFor(customerId);
        ctx.setDailyTransferCount(stats.count());
        ctx.setDailyTransferTotal(stats.total());
        ctx.setKnownCounterparty(statsProvider.isKnownCounterparty(customerId, transfer.getToAccountId()));
        ctx.setHighRiskCountry(false);

        KieSession session = kieContainer.newKieSession();
        try {
            session.setGlobal("logger", LoggerFactory.getLogger("drools.fraud"));
            session.insert(ctx);
            int fired = session.fireAllRules();
            log.debug("Drools fired {} rules for transfer {}", fired, transfer.getId());
        } finally {
            session.dispose();
        }
        return ctx.getAlerts();
    }
}
