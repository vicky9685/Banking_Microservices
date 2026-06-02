package com.bank.workflow.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

/**
 * Callback into the transaction-service when a workflow decision is reached.
 * The transaction-service exposes /api/transfers/{id}/decision to consume these.
 */
@FeignClient(name = "transaction-service", path = "/api/transfers")
public interface TransferClient {

    @PostMapping("/{id}/decision")
    ApiResponse<Map<String, Object>> decide(@PathVariable("id") UUID id,
                                            @RequestParam("approved") boolean approved,
                                            @RequestParam(value = "comment", required = false) String comment);
}
