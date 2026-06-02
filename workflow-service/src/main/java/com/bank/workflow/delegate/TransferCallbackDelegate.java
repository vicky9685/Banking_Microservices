package com.bank.workflow.delegate;

import com.bank.workflow.client.TransferClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Final step on the approve branch: calls back to transaction-service to resume the saga.
 */
@Slf4j
@Component("transferCallbackDelegate")
@RequiredArgsConstructor
public class TransferCallbackDelegate implements JavaDelegate {

    private final TransferClient transferClient;

    @Override
    public void execute(DelegateExecution exec) {
        UUID transferId = UUID.fromString((String) exec.getVariable("transferId"));
        String comment = (String) exec.getVariable("comment");
        transferClient.decide(transferId, true, comment);
        log.info("Resumed transfer {} (approved)", transferId);
    }
}
