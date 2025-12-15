package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.infrastructure.persistence.entity.RateLimitTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for Rate Limit tracking
 */
@Repository
public interface RateLimitTrackingRepository extends JpaRepository<RateLimitTrackingEntity, String> {

    Optional<RateLimitTrackingEntity> findByConsumerIdAndWindowStart(String consumerId, LocalDateTime windowStart);

    @Query("SELECT SUM(r.requestCount) FROM RateLimitTrackingEntity r WHERE r.consumerId = :consumerId AND r.windowStart >= :since")
    Integer sumRequestCountByConsumerIdAndWindowStartAfter(String consumerId, LocalDateTime since);
}

