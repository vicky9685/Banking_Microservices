package com.bank.workflow.delegate;

import com.bank.workflow.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component("kycCheckDelegate")
@RequiredArgsConstructor
public class KycCheckDelegate implements JavaDelegate {

    private final CustomerClient customerClient;

    @Override
    public void execute(DelegateExecution exec) {
        UUID customerId = UUID.fromString((String) exec.getVariable("customerId"));
        Map<String, Object> c = customerClient.getCustomer(customerId).getData();
        exec.setVariable("kycStatus", c.getOrDefault("kycStatus", "UNKNOWN"));
    }
}
