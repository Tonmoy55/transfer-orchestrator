package com.company.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Audit events (append-only)
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_transfer_id", columnList = "transferId"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transferId;

    @Column(nullable = false)
    private String eventType;

    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 2000)
    private String details;

    @Column(length = 5000)
    private String metadata;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}

