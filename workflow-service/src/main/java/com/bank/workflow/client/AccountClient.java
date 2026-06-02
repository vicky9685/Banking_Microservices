package com.bank.workflow.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "account-service", path = "/api/accounts")
public interface AccountClient {
    record CreateLoanAccount(UUID customerId, String type, String currency, BigDecimal initialDeposit) {}

    @PostMapping
    ApiResponse<Map<String, Object>> open(@RequestBody CreateLoanAccount body);
}
