package com.bank.account.service;

import com.bank.account.client.CustomerClient;
import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.account.dto.AccountDtos.*;
import com.bank.account.repository.AccountRepository;
import com.bank.common.dto.ApiResponse;
import com.bank.common.exception.BusinessException;
import com.bank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Account application service.
 * - Validates customer existence via Feign (synchronous, cross-service consistency).
 * - All balance mutations go through the aggregate's debit/credit invariants.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final AccountNumberGenerator numberGenerator;
    private final CustomerClient customerClient;

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        ApiResponse<Map<String, Object>> customer = customerClient.getCustomer(request.customerId());
        if (customer == null || !customer.isSuccess()) {
            throw new BusinessException("CUSTOMER_INVALID",
                    "Cannot open account: " + (customer == null ? "no response" : customer.getMessage()));
        }
        if (!"VERIFIED".equals(customer.getData().get("kycStatus"))) {
            throw new BusinessException("KYC_REQUIRED", "Customer KYC must be VERIFIED to open an account");
        }

        Account account = Account.builder()
                .accountNumber(numberGenerator.generate())
                .customerId(request.customerId())
                .type(request.type())
                .status(AccountStatus.ACTIVE)
                .balance(request.initialDeposit() == null ? BigDecimal.ZERO : request.initialDeposit())
                .currency(request.currency())
                .build();
        Account saved = repository.save(account);
        log.info("Account opened number={} customer={}", saved.getAccountNumber(), saved.getCustomerId());
        return toResponse(saved);
    }

    @Cacheable(cacheNames = "accounts:by-id", key = "#id")
    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    @Cacheable(cacheNames = "accounts:by-customer", key = "#customerId")
    @Transactional(readOnly = true)
    public List<AccountResponse> listByCustomer(UUID customerId) {
        return repository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "accounts:by-id", key = "#id"),
        @CacheEvict(cacheNames = "accounts:by-customer", allEntries = true)
    })
    @Transactional
    public AccountResponse deposit(UUID id, BalanceAdjustment adj) {
        Account account = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.credit(adj.amount());
        return toResponse(repository.save(account));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "accounts:by-id", key = "#id"),
        @CacheEvict(cacheNames = "accounts:by-customer", allEntries = true)
    })
    @Transactional
    public AccountResponse withdraw(UUID id, BalanceAdjustment adj) {
        Account account = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.debit(adj.amount());
        return toResponse(repository.save(account));
    }

    @CacheEvict(cacheNames = "accounts:by-id", key = "#id")
    @Transactional
    public AccountResponse setStatus(UUID id, AccountStatus status) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.setStatus(status);
        return toResponse(repository.save(account));
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomerId(),
                a.getType(), a.getStatus(), a.getBalance(), a.getCurrency(), a.getCreatedAt());
    }
}
