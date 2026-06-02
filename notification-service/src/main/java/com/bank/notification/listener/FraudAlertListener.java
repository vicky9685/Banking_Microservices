package com.bank.notification.listener;

import com.bank.common.events.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to ALL fraud alerts via wildcard. In production this would split
 * channels (SMS for CRITICAL, push for HIGH, queue for analyst review for the rest).
 * Solace wildcards: '>' = match remainder; '*' = single level.
 */
@Slf4j
@Component
public class FraudAlertListener {

    @JmsListener(
            destination = "banking/fraud/alerts/v1/>",
            containerFactory = "solaceListenerFactory")
    public void onFraudAlert(FraudAlert alert) {
        switch (alert.severity()) {
            case CRITICAL -> dispatchSms(alert);
            case HIGH     -> dispatchPush(alert);
            case MEDIUM, LOW -> queueForReview(alert);
        }
    }

    private void dispatchSms(FraudAlert a) {
        log.warn("[FRAUD][SMS] customer={} amount={} rule={}", a.customerId(), a.amount(), a.rule());
    }
    private void dispatchPush(FraudAlert a) {
        log.info("[FRAUD][PUSH] customer={} amount={} rule={}", a.customerId(), a.amount(), a.rule());
    }
    private void queueForReview(FraudAlert a) {
        log.info("[FRAUD][REVIEW] alertId={} customer={}", a.alertId(), a.customerId());
    }
}
