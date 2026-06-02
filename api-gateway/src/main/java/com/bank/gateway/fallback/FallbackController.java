package com.bank.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping({"/customers", "/accounts", "/transactions"})
    public ResponseEntity<Map<String, Object>> getFallback() {
        return degraded();
    }

    @PostMapping({"/customers", "/accounts", "/transactions"})
    public ResponseEntity<Map<String, Object>> postFallback() {
        return degraded();
    }

    private ResponseEntity<Map<String, Object>> degraded() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "errorCode", "SERVICE_UNAVAILABLE",
                        "message", "Downstream temporarily unavailable, please retry"
                ));
    }
}
