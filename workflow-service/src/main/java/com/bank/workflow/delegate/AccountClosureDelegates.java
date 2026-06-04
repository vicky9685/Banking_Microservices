package com.bank.workflow.delegate;

import com.bank.common.dto.ApiResponse;
import com.bank.workflow.client.AccountClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Service-task delegates used by account-closure.bpmn. Grouped here to keep the
 * one-file-per-process convention.
 */
@Configuration
public class AccountClosureDelegates {

    /** Extra Feign client just for closure actions on account-service. */
    @FeignClient(name = "account-service-closure", url = "${app.callbacks.account-service.url}", path = "/api/accounts")
    public interface AccountOps {
        @PostMapping("/{id}/status/{status}")
        ApiResponse<Map<String, Object>> setStatus(@PathVariable("id") UUID id, @PathVariable("status") String status);

        @GetMapping("/{id}")
        ApiResponse<Map<String, Object>> get(@PathVariable("id") UUID id);

        record Adjust(BigDecimal amount, String reason) {}

        @PostMapping("/{id}/withdraw")
        ApiResponse<Map<String, Object>> withdraw(@PathVariable("id") UUID id, @RequestBody Adjust body);
    }

    @Slf4j
    @Component("freezeAccountDelegate")
    @RequiredArgsConstructor
    public static class Freeze implements JavaDelegate {
        private final AccountOps ops;
        @Override public void execute(DelegateExecution ex) {
            UUID id = UUID.fromString((String) ex.getVariable("accountId"));
            ops.setStatus(id, "FROZEN");
            log.info("Closure pi={} froze account={}", ex.getProcessInstanceId(), id);
        }
    }

    @Slf4j
    @Component("balanceCheckDelegate")
    @RequiredArgsConstructor
    public static class BalanceCheck implements JavaDelegate {
        private final AccountOps ops;
        @Override public void execute(DelegateExecution ex) {
            UUID id = UUID.fromString((String) ex.getVariable("accountId"));
            Map<String, Object> acct = ops.get(id).getData();
            Number bal = (Number) acct.getOrDefault("balance", 0);
            ex.setVariable("residualBalance", bal.longValue());
            String currency = (String) acct.getOrDefault("currency", "USD");
            ex.setVariable("currency", currency);
            // Default cooling-off: 7 days in production, configurable via input variable for tests.
            if (ex.getVariable("coolingOffDuration") == null) {
                ex.setVariable("coolingOffDuration", "P7D");
            }
        }
    }

    @Slf4j
    @Component("disburseResidualDelegate")
    @RequiredArgsConstructor
    public static class Disburse implements JavaDelegate {
        private final AccountOps ops;
        @Override public void execute(DelegateExecution ex) {
            UUID id = UUID.fromString((String) ex.getVariable("accountId"));
            Number amount = (Number) ex.getVariable("residualBalance");
            ops.withdraw(id, new AccountOps.Adjust(new BigDecimal(amount.toString()), "Closure disbursement"));
            log.info("Disbursed residual {} from {}", amount, id);
        }
    }

    @Slf4j
    @Component("closeAccountDelegate")
    @RequiredArgsConstructor
    public static class Close implements JavaDelegate {
        private final AccountOps ops;
        @Override public void execute(DelegateExecution ex) {
            UUID id = UUID.fromString((String) ex.getVariable("accountId"));
            ops.setStatus(id, "CLOSED");
            log.info("Account {} CLOSED via pi={}", id, ex.getProcessInstanceId());
        }
    }

    @Slf4j
    @Component("unfreezeAccountDelegate")
    @RequiredArgsConstructor
    public static class Unfreeze implements JavaDelegate {
        private final AccountOps ops;
        @Override public void execute(DelegateExecution ex) {
            UUID id = UUID.fromString((String) ex.getVariable("accountId"));
            ops.setStatus(id, "ACTIVE");
            log.info("Account {} re-activated (closure cancelled) via pi={}", id, ex.getProcessInstanceId());
        }
    }

    /** Used by AccountClient interface — declared here to avoid duplicating. */
    public static void notUsed(AccountClient c) { /* keep import warning silent */ }
}
