package com.bank.account.dto;

import com.bank.account.domain.AccountStatus;
import com.bank.account.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() {}

    public record CreateAccountRequest(
            @NotNull UUID customerId,
            @NotNull AccountType type,
            @NotNull String currency,
            @DecimalMin(value = "0.0", inclusive = true) BigDecimal initialDeposit) {}

    public record AccountResponse(
            UUID id, String accountNumber, UUID customerId,
            AccountType type, AccountStatus status,
            BigDecimal balance, String currency,
            Instant createdAt) {}

    public record BalanceAdjustment(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String reason) {}
}
