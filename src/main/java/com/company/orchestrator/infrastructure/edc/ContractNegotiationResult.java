package com.company.orchestrator.infrastructure.edc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of EDC contract negotiation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractNegotiationResult {
    private String negotiationId;
    private String agreementId;
    private boolean success;
    private String message;
}

