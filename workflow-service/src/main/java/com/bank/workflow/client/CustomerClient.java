package com.bank.workflow.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "customer-service", path = "/api/customers")
public interface CustomerClient {
    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getCustomer(@PathVariable("id") UUID id);
}
