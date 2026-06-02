package com.bank.transaction.dto;

import com.bank.transaction.domain.TransferStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransferDtos {
    private TransferDtos() {}

    public record TransferRequest(
            @NotNull UUID fromAccountId,
            @NotNull UUID toAccountId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull String currency,
            String idempotencyKey) {}

    public record TransferResponse(
            UUID id, UUID fromAccountId, UUID toAccountId,
            BigDecimal amount, String currency,
            TransferStatus status, String failureReason,
            Instant createdAt) {}
}
