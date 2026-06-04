package com.bank.account.saga;

import com.bank.account.domain.Account;
import com.bank.account.repository.AccountRepository;
import com.bank.common.events.KafkaTopics;
import com.bank.common.events.TransactionEvents.*;
import com.bank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga participant on the Account side.
 * Listens for orchestrator commands wrapped as events on TRANSFER_COMMANDS
 * and emits results to TRANSFER_EVENTS. Compensations are explicit credit-backs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransferSagaListener {

    private final AccountRepository repository;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = KafkaTopics.TRANSFER_COMMANDS, groupId = "account-service")
    @Transactional
    public void onCommand(Object payload) {
        switch (payload) {
            case TransferInitiated cmd -> handleDebit(cmd);
            case AccountDebited cmd    -> handleCredit(cmd);
            case CompensationRequested cmd -> handleCompensation(cmd);
            default -> log.debug("Ignoring unrelated event: {}", payload.getClass());
        }
    }

    private void handleDebit(TransferInitiated cmd) {
        try {
            Account from = repository.findByIdForUpdate(cmd.fromAccountId())
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND",
                            "From account: " + cmd.fromAccountId()));
            from.debit(cmd.amount());
            repository.save(from);
            kafka.send(KafkaTopics.TRANSFER_EVENTS, cmd.transferId().toString(),
                    new AccountDebited(UUID.randomUUID(), Instant.now(),
                            cmd.transferId().toString(), cmd.transferId(),
                            cmd.fromAccountId(), cmd.amount()));
        } catch (BusinessException ex) {
            log.warn("Debit failed for transfer {}: {}", cmd.transferId(), ex.getMessage());
            kafka.send(KafkaTopics.TRANSFER_EVENTS, cmd.transferId().toString(),
                    new TransferFailed(UUID.randomUUID(), Instant.now(),
                            cmd.transferId().toString(), cmd.transferId(), ex.getMessage()));
        }
    }

    private void handleCredit(AccountDebited cmd) {
        // The Transaction orchestrator turns AccountDebited into a credit command on the other side.
        // For simplicity in this single-listener variant we wait for an explicit credit command from
        // the orchestrator (modeled below as the same event redirected).
    }

    private void handleCompensation(CompensationRequested cmd) {
        Account acct = repository.findByIdForUpdate(cmd.accountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND",
                        "Compensation target: " + cmd.accountId()));
        acct.credit(cmd.amount());
        repository.save(acct);
        log.info("Compensated transfer={} account={} amount={}",
                cmd.transferId(), cmd.accountId(), cmd.amount());
    }
}
