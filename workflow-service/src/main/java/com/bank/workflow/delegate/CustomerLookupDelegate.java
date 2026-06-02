package com.bank.workflow.delegate;

import com.bank.workflow.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service task delegate: looks up the customer's tier so a downstream user task
 * can route to the right approver group, and pre-computes a due date.
 */
@Slf4j
@Component("customerLookupDelegate")
@RequiredArgsConstructor
public class CustomerLookupDelegate implements JavaDelegate {

    private final CustomerClient customerClient;

    @Override
    public void execute(DelegateExecution exec) {
        UUID customerId = UUID.fromString((String) exec.getVariable("customerId"));
        Map<String, Object> customer = customerClient.getCustomer(customerId).getData();
        String kyc = (String) customer.getOrDefault("kycStatus", "UNKNOWN");
        exec.setVariable("kycStatus", kyc);
        exec.setVariable("customerTier", "STANDARD"); // hook for tiering service
        // 30-minute SLA visible to Cockpit
        exec.setVariable("dueDate", Date.from(Instant.now().plus(Duration.ofMinutes(30))));
        log.info("Customer {} looked up: kyc={}", customerId, kyc);
    }
}
