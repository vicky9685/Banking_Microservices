package com.bank.account.api;

import com.bank.account.domain.AccountStatus;
import com.bank.account.dto.AccountDtos.*;
import com.bank.account.service.AccountService;
import com.bank.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> open(@Valid @RequestBody CreateAccountRequest req) {
        return ApiResponse.ok(service.open(req), "Account opened");
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<AccountResponse>> listByCustomer(@PathVariable UUID customerId) {
        return ApiResponse.ok(service.listByCustomer(customerId));
    }

    @PostMapping("/{id}/deposit")
    public ApiResponse<AccountResponse> deposit(@PathVariable UUID id,
                                                @Valid @RequestBody BalanceAdjustment adj) {
        return ApiResponse.ok(service.deposit(id, adj));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<AccountResponse> withdraw(@PathVariable UUID id,
                                                 @Valid @RequestBody BalanceAdjustment adj) {
        return ApiResponse.ok(service.withdraw(id, adj));
    }

    @PostMapping("/{id}/status/{status}")
    public ApiResponse<AccountResponse> setStatus(@PathVariable UUID id,
                                                  @PathVariable AccountStatus status) {
        return ApiResponse.ok(service.setStatus(id, status));
    }
}
