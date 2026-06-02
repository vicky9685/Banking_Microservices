package com.bank.customer.dto;

import com.bank.customer.domain.KycStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input/output records. Records give us immutability + compact syntax.
 */
public final class CustomerDtos {
    private CustomerDtos() {}

    public record AddressDto(String line1, String line2, String city,
                             String state, String postalCode, String country) {}

    public record CreateCustomerRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email,
            @NotBlank String phone,
            @NotBlank String nationalId,
            @Past LocalDate dateOfBirth,
            AddressDto address) {}

    public record UpdateCustomerRequest(
            String firstName,
            String lastName,
            String phone,
            AddressDto address) {}

    public record CustomerResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String nationalId,
            LocalDate dateOfBirth,
            KycStatus kycStatus,
            AddressDto address,
            Instant createdAt,
            Instant updatedAt) {}
}
