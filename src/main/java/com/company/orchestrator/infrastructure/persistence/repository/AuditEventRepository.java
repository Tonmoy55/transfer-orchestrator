package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.infrastructure.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit events
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    List<AuditEventEntity> findByTransferIdOrderByTimestampAsc(String transferId);

    List<AuditEventEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    Page<AuditEventEntity> findByTransferId(String transferId, Pageable pageable);
}

