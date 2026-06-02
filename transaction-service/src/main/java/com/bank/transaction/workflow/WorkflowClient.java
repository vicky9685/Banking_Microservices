package com.bank.transaction.workflow;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "workflow-service", path = "/api/workflows", fallback = WorkflowClient.Fallback.class)
public interface WorkflowClient {

    record HighValueRequest(UUID transferId, UUID customerId, BigDecimal amount, String currency) {}

    @PostMapping("/high-value-approval")
    ApiResponse<Map<String, Object>> startHighValueApproval(@RequestBody HighValueRequest body);

    class Fallback implements WorkflowClient {
        @Override
        public ApiResponse<Map<String, Object>> startHighValueApproval(HighValueRequest body) {
            return ApiResponse.error("WORKFLOW_UNAVAILABLE", "Workflow service unavailable; transfer held");
        }
    }
}
