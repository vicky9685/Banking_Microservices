package com.bank.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, java.util.UUID> {
    @Query("select max(a.sequence) from AuditEvent a")
    Optional<Long> maxSequence();

    AuditEvent findTopByOrderBySequenceDesc();
}
