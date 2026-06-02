package com.bank.account.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Synchronous client to Customer Service via Feign + Eureka load-balancing.
 * Wrapped with circuit breaker by Resilience4j auto-config.
 */
@FeignClient(name = "customer-service", path = "/api/customers", fallback = CustomerClient.Fallback.class)
public interface CustomerClient {

    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getCustomer(@PathVariable("id") UUID id);

    class Fallback implements CustomerClient {
        @Override
        public ApiResponse<Map<String, Object>> getCustomer(UUID id) {
            return ApiResponse.error("CUSTOMER_SERVICE_DOWN", "Cannot validate customer right now");
        }
    }
}
