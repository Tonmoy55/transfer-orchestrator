package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.infrastructure.persistence.entity.TransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Transfer entities
 */
@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, String> {

    Page<TransferEntity> findByCurrentState(TransferState state, Pageable pageable);

    List<TransferEntity> findByCurrentStateIn(List<TransferState> states);

    List<TransferEntity> findByConsumerIdAndCreatedAtAfter(String consumerId, LocalDateTime after);

    @Query("SELECT COUNT(t) FROM TransferEntity t WHERE t.currentState = :state")
    Long countByState(TransferState state);
}
