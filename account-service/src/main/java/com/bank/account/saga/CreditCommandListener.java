package com.bank.account.saga;

import com.bank.account.domain.Account;
import com.bank.account.repository.AccountRepository;
import com.bank.common.events.KafkaTopics;
import com.bank.common.events.TransactionEvents.AccountCredited;
import com.bank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Separate listener bound to a dedicated topic for credit commands.
 * Keeps debit/credit flow obvious in logs and lets us scale consumers independently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCommandListener {

    public static final String CREDIT_COMMANDS = "banking.transfer.credit.commands";

    private final AccountRepository repository;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = CREDIT_COMMANDS, groupId = "account-service-credit")
    @Transactional
    public void onCreditCommand(AccountCredited cmd) {
        try {
            Account to = repository.findByIdForUpdate(cmd.accountId())
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND",
                            "To account: " + cmd.accountId()));
            to.credit(cmd.amount());
            repository.save(to);
            kafka.send(KafkaTopics.TRANSFER_EVENTS, cmd.transferId().toString(),
                    new AccountCredited(UUID.randomUUID(), Instant.now(),
                            cmd.transferId().toString(), cmd.transferId(),
                            cmd.accountId(), cmd.amount()));
        } catch (BusinessException ex) {
            log.warn("Credit failed transfer={}: {}", cmd.transferId(), ex.getMessage());
            // Orchestrator will see absence + timeout, or we could publish a TransferFailed here.
            throw ex;
        }
    }
}
