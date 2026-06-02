package com.bank.transaction.fraud;

import com.bank.common.events.FraudAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes fraud alerts to Solace. Topic layout:
 *   banking/fraud/alerts/v1/{severity}/{customerId}
 *
 * Downstream subscribers can match on wildcards, e.g.
 *   banking/fraud/alerts/v1/CRITICAL/>
 *   banking/fraud/alerts/v1/>/cust-abc-123
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAlertPublisher {

    private final JmsTemplate solaceJmsTemplate;

    @Value("${app.solace.fraud-alerts-topic:banking/fraud/alerts/v1}")
    private String basePrefix;

    public void publish(FraudAlert alert) {
        String destination = "%s/%s/%s".formatted(basePrefix, alert.severity(), alert.customerId());
        solaceJmsTemplate.convertAndSend(destination, alert);
        log.info("Fraud alert published topic={} severity={} customer={}",
                destination, alert.severity(), alert.customerId());
    }
}
