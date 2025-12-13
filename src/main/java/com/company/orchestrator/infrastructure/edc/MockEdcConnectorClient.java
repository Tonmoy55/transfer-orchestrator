package com.company.orchestrator.infrastructure.edc;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of EDC Connector Client
 * In production, this would use HTTP client to call actual EDC Management API
 */
@Component
@Slf4j
public class MockEdcConnectorClient implements EdcConnectorClient {

    // In-memory store for mock state
    private final Map<String, TransferProcessState> transferStates = new ConcurrentHashMap<>();
    private final Map<String, String> agreementIds = new ConcurrentHashMap<>();

    @Override
    public ContractNegotiationResult negotiateContract(ContractOffer offer) {
        log.info("Mock EDC: Negotiating contract for transferId: {}, asset: {}", offer.getTransferId(), offer.getAssetId());

        // Simulate successful negotiation
        String negotiationId = UUID.randomUUID().toString();
        String agreementId = "agreement-" + UUID.randomUUID();

        agreementIds.put(negotiationId, agreementId);

        log.debug("Mock EDC: Contract negotiation successful. Transfer ID: {}, Agreement ID: {}", offer.getTransferId(), agreementId);

        return ContractNegotiationResult.builder()
            .negotiationId(negotiationId)
            .agreementId(agreementId)
            .success(true)
            .message("Contract negotiation completed successfully")
            .build();
    }

    @Override
    public TransferProcessResult initiateTransfer(String agreementId, TransferRequest request) {
        log.info("Mock EDC: Initiating transfer for transferId: {}, agreement: {}", request.getTransferId(), agreementId);

        // Simulate successful transfer initiation
        String transferProcessId = "transfer-" + UUID.randomUUID();
        transferStates.put(transferProcessId, TransferProcessState.STARTED);

        log.debug("Mock EDC: Transfer initiated. TransferId: {}, Process ID: {}", request.getTransferId(), transferProcessId);

        return TransferProcessResult.builder()
            .transferProcessId(transferProcessId)
            .success(true)
            .message("Transfer process started successfully")
            .build();
    }

    @Override
    public TransferProcessState getTransferState(String transferProcessId) {
        log.debug("Mock EDC: Getting transfer state for process: {}", transferProcessId);

        TransferProcessState state = transferStates.getOrDefault(
            transferProcessId,
            TransferProcessState.COMPLETED
        );

        // Simulate state progression
        if (state == TransferProcessState.STARTED) {
            // Automatically mark as completed after first check
            transferStates.put(transferProcessId, TransferProcessState.COMPLETED);
            state = transferStates.getOrDefault(transferProcessId, TransferProcessState.COMPLETED);
        }

        return state;
    }

    @Override
    public void terminateTransfer(String transferProcessId) {
        log.info("Mock EDC: Terminating transfer process: {}", transferProcessId);

        transferStates.put(transferProcessId, TransferProcessState.TERMINATED);

        log.debug("Mock EDC: Transfer terminated successfully");
    }

    /**
     * Helper method for testing - simulate transfer failure
     */
    public void simulateFailure(String transferProcessId) {
        transferStates.put(transferProcessId, TransferProcessState.ERROR);
    }
}

