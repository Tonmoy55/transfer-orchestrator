package com.company.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for tracking rate limits per consumer
 */
@Entity
@Table(name = "rate_limit_tracking", indexes = {
    @Index(name = "idx_consumer_window", columnList = "consumerId, windowStart")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String consumerId;

    @Column(nullable = false)
    private LocalDateTime windowStart;

    @Column(nullable = false)
    private Integer requestCount;

    @Column(nullable = false)
    private LocalDateTime lastRequest;
}

