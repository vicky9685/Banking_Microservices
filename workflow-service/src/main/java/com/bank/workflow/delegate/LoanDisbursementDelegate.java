package com.bank.workflow.delegate;

import com.bank.workflow.client.AccountClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Opens a LOAN account funded with the approved principal.
 * On failure, BPMN can route to a compensation task; left as an integration point.
 */
@Slf4j
@Component("loanDisbursementDelegate")
@RequiredArgsConstructor
public class LoanDisbursementDelegate implements JavaDelegate {

    private final AccountClient accountClient;

    @Override
    public void execute(DelegateExecution exec) {
        UUID customerId = UUID.fromString((String) exec.getVariable("customerId"));
        Number amount = (Number) exec.getVariableLocal("adjustedAmount");
        if (amount == null) amount = (Number) exec.getVariable("amount");
        BigDecimal principal = new BigDecimal(amount.toString());
        String currency = (String) exec.getVariableLocal("currency");
        if (currency == null) currency = "USD";

        Map<String, Object> result = accountClient.open(
                new AccountClient.CreateLoanAccount(customerId, "LOAN", currency, principal)
        ).getData();
        exec.setVariable("loanAccountId", result.get("id"));
        log.info("Loan disbursed customer={} principal={} account={}", customerId, principal, result.get("id"));
    }
}
