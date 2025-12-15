package com.company.orchestrator.infrastructure.edc;

/**
 * EDC Transfer Process States
 */
public enum TransferProcessState {
    INITIAL,
    PROVISIONED,
    REQUESTED,
    STARTED,
    COMPLETED,
    SUSPENDED,
    TERMINATED,
    ERROR
}

