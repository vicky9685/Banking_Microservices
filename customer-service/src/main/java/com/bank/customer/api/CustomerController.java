package com.bank.customer.api;

import com.bank.common.dto.ApiResponse;
import com.bank.customer.domain.KycStatus;
import com.bank.customer.dto.CustomerDtos.*;
import com.bank.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a new customer")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.create(request), "Customer onboarded");
    }

    /**
     * Ownership rule: a CUSTOMER role can only read their own record. Back-office
     * roles (KYC_OFFICER, COMPLIANCE) bypass via SpEL.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Fetch a customer by id")
    @PreAuthorize("hasAnyRole('KYC_OFFICER','COMPLIANCE') or principal.ownsCustomer(#id)")
    public ApiResponse<CustomerResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('KYC_OFFICER') or principal.ownsCustomer(#id)")
    public ApiResponse<CustomerResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @PostMapping("/{id}/kyc/{status}")
    @Operation(summary = "Update KYC status (back-office)")
    @PreAuthorize("hasAnyRole('KYC_OFFICER','COMPLIANCE')")
    public ApiResponse<CustomerResponse> updateKyc(@PathVariable UUID id,
                                                   @PathVariable KycStatus status) {
        return ApiResponse.ok(customerService.updateKyc(id, status));
    }
}
