package com.bank.notification.listener;

import com.bank.common.events.KafkaTopics;
import com.bank.common.events.TransactionEvents.TransferCompleted;
import com.bank.common.events.TransactionEvents.TransferFailed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to domain events and dispatches notifications.
 * Real channels (email/SMS/push) would plug into the dispatch methods below.
 */
@Slf4j
@Component
public class NotificationListener {

    @KafkaListener(topics = KafkaTopics.NOTIFICATIONS, groupId = "notification-service")
    public void onNotificationEvent(Object event) {
        switch (event) {
            case TransferCompleted e -> dispatchSuccess(e);
            case TransferFailed e -> dispatchFailure(e);
            default -> log.debug("No template for {}", event.getClass());
        }
    }

    private void dispatchSuccess(TransferCompleted e) {
        log.info("[NOTIFY] Transfer {} completed — sending success email/SMS", e.transferId());
    }

    private void dispatchFailure(TransferFailed e) {
        log.info("[NOTIFY] Transfer {} failed: {}", e.transferId(), e.reason());
    }
}
