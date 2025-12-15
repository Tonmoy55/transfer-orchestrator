package com.company.orchestrator.domain.enums;

/**
 * Transfer lifecycle states
 */
public enum TransferState {
    REQUESTED,
    POLICY_EVALUATION,
    APPROVED,
    DENIED,
    CONTRACT_NEGOTIATION,
    NEGOTIATED,
    TRANSFER_IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
