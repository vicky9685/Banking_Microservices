package com.bank.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub credit bureau call. Production: call Experian/Equifax via a credentialed client.
 */
@Slf4j
@Component("creditCheckDelegate")
public class CreditCheckDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution exec) {
        UUID customerId = UUID.fromString((String) exec.getVariable("customerId"));
        // Deterministic synthetic score so demos are reproducible.
        int score = 500 + Math.abs(customerId.hashCode() % 350);
        exec.setVariable("creditScore", score);
        log.info("Credit score for {} = {}", customerId, score);
    }
}
