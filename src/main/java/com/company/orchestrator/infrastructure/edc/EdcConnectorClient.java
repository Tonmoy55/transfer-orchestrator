package com.company.orchestrator.infrastructure.edc;

import com.company.orchestrator.domain.model.TransferRequest;

/**
 * Client interfaces for EDC Management API
 * This is a mock implementation - in production, this would call actual EDC endpoints
 */
public interface EdcConnectorClient {

    /**
     * Negotiates contract with consumer connector
     */
    ContractNegotiationResult negotiateContract(ContractOffer offer);

    /**
     * Initiates data transfer after successful negotiation
     */
    TransferProcessResult initiateTransfer(String agreementId, TransferRequest request);

    /**
     * Monitors transfer process status
     */
    TransferProcessState getTransferState(String transferProcessId);

    /**
     * Cancels ongoing transfer
     */
    void terminateTransfer(String transferProcessId);
}

