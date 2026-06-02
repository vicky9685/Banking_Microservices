package com.bank.customer.service;

import com.bank.common.exception.BusinessException;
import com.bank.common.exception.ResourceNotFoundException;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.KycStatus;
import com.bank.customer.dto.CustomerDtos.*;
import com.bank.customer.mapper.CustomerMapper;
import com.bank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service. Coordinates domain rules and persistence.
 * Cached read path; writes evict the cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already registered");
        }
        if (repository.existsByNationalId(request.nationalId())) {
            throw new BusinessException("DUPLICATE_NATIONAL_ID", "National ID already registered");
        }
        Customer entity = mapper.toEntity(request);
        Customer saved = repository.save(entity);
        log.info("Customer created id={} email={}", saved.getId(), saved.getEmail());
        return mapper.toResponse(saved);
    }

    @Cacheable(cacheNames = "customers", key = "#id")
    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @CacheEvict(cacheNames = "customers", key = "#id")
    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        Customer existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        mapper.updateEntity(request, existing);
        return mapper.toResponse(repository.save(existing));
    }

    @CacheEvict(cacheNames = "customers", key = "#id")
    @Transactional
    public CustomerResponse updateKyc(UUID id, KycStatus status) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        customer.setKycStatus(status);
        log.info("KYC status updated id={} status={}", id, status);
        return mapper.toResponse(repository.save(customer));
    }
}
