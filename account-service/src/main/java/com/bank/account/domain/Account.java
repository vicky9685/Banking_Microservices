package com.bank.account.domain;

import com.bank.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Account aggregate. Enforces invariants (non-negative balance, status checks)
 * inside the domain object — service layer cannot bypass them.
 * Optimistic locking via @Version protects against lost updates under concurrency.
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number", unique = true),
        @Index(name = "idx_account_customer", columnList = "customer_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id @GeneratedValue
    private UUID id;

    @Setter
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Setter
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Setter
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Setter
    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    private Long version;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Business rule: cannot debit closed/frozen accounts and cannot overdraw. */
    public void debit(BigDecimal amount) {
        ensureActive();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Debit amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_FUNDS",
                    "Insufficient balance on account " + accountNumber);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        ensureActive();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE",
                    "Account %s is %s".formatted(accountNumber, status));
        }
    }
}
