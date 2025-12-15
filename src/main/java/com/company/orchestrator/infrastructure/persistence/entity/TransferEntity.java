package com.company.orchestrator.infrastructure.persistence.entity;

import com.company.orchestrator.domain.enums.TransferState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Transfer persistence
 */
@Entity
@Table(name = "transfers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String consumerId;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String assetId;

    private String dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferState currentState;

    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Builder.Default
    private Integer retryCount = 0;

    private String edcTransferProcessId;

    private String edcContractAgreementId;

    private String consumerRegion;

    private String consumerCertificationLevel;

    private String usagePurpose;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
