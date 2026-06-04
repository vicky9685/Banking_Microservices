package com.bank.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Delegates for dispute-resolution.bpmn.
 *
 * In a real bank these would persist case files, post journal entries, and
 * write to the regulator-facing dispute ledger. Stubs keep the surface obvious.
 */
@Configuration
public class DisputeDelegates {

    @Slf4j
    @Component("disputeOpenDelegate")
    public static class Open implements JavaDelegate {
        @Override public void execute(DelegateExecution ex) {
            ex.setVariable("caseId", "DSP-" + ex.getProcessInstanceId().substring(0, 8));
            ex.setVariable("dueDate", Date.from(Instant.now().plus(Duration.ofDays(5))));
            log.info("Dispute case {} opened (transfer={}, customer={})",
                    ex.getVariable("caseId"),
                    ex.getVariable("transferId"),
                    ex.getVariable("customerId"));
        }
    }

    @Slf4j
    @Component("disputeRefundDelegate")
    public static class Refund implements JavaDelegate {
        @Override public void execute(DelegateExecution ex) {
            log.info("Refunding dispute case={} amount={}",
                    ex.getVariable("caseId"), ex.getVariable("amount"));
            // Hook: call account-service.deposit to credit customer's account.
        }
    }

    @Slf4j
    @Component("disputeCloseDelegate")
    public static class Close implements JavaDelegate {
        @Override public void execute(DelegateExecution ex) {
            log.info("Closing dispute case={} as REJECTED. Notes: {}",
                    ex.getVariable("caseId"), ex.getVariable("notes"));
        }
    }

    @Slf4j
    @Component("disputeSlaBreachDelegate")
    public static class SlaBreach implements JavaDelegate {
        @Override public void execute(DelegateExecution ex) {
            log.warn("SLA breached on dispute case={} (5-day target). Manager notified.",
                    ex.getVariable("caseId"));
        }
    }
}
