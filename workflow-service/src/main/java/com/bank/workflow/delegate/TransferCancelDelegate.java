package com.bank.workflow.delegate;

import com.bank.workflow.client.TransferClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("transferCancelDelegate")
@RequiredArgsConstructor
public class TransferCancelDelegate implements JavaDelegate {

    private final TransferClient transferClient;

    @Override
    public void execute(DelegateExecution exec) {
        UUID transferId = UUID.fromString((String) exec.getVariable("transferId"));
        String comment = (String) exec.getVariable("comment");
        transferClient.decide(transferId, false, comment);
        log.info("Cancelled transfer {} (rejected)", transferId);
    }
}
