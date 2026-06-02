package com.bank.transaction.repository;

import com.bank.transaction.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("select o from OutboxEvent o where o.processed = false order by o.createdAt asc")
    List<OutboxEvent> findUnpublished(Pageable pageable);
}
