package com.bank.customer.repository;

import com.bank.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pattern. Spring Data derives queries from method names.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
}
