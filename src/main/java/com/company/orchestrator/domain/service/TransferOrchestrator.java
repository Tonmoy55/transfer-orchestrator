package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.*;
import com.company.orchestrator.infrastructure.edc.*;
import com.company.orchestrator.infrastructure.persistence.entity.TransferEntity;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Transfer Orchestration Service
 * Critical Component: Orchestrates multi-step transfer workflows with error handling
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransferOrchestrator {

    private final TransferRepository transferRepository;
    private final PolicyEvaluationService policyEvaluationService;
    private final AuditService auditService;
    private final EdcConnectorClient edcConnectorClient;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_BACKOFF_MS = 1000;

    /**
     * Initiates a data transfer with policy evaluation
     */
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer: Consumer={}, Asset={}",
            request.getConsumerId(), request.getAssetId());

        try {
            // Step 1: Create transfer entity (without ID - let JPA generate it)
            TransferEntity transfer = createTransferEntity(request);
            transfer = transferRepository.save(transfer);

            String transferId = transfer.getId(); // Get the generated ID

            log.info("Transfer created with ID: {}", transferId);

            // Step 2: Log request
            auditService.logTransferRequest(transferId, request);

            // Step 3: Evaluate policies
            transfer.setCurrentState(TransferState.POLICY_EVALUATION);
            auditService.logStateTransition(transferId, TransferState.REQUESTED,
                TransferState.POLICY_EVALUATION, "Starting policy evaluation");

            PolicyEvaluationResult policyResult = policyEvaluationService.evaluateAll(request);
            auditService.logPolicyEvaluation(transferId, policyResult);

            if (!policyResult.isAllowed()) {
                // Policy denied
                transfer.setCurrentState(TransferState.DENIED);
                transfer.setMessage(policyResult.getReason());
                // Final save at transaction commit

                auditService.logStateTransition(transferId, TransferState.POLICY_EVALUATION,
                    TransferState.DENIED, policyResult.getReason());

                log.warn("Transfer denied due to policy violations: {}", transferId);

                return TransferResponse.builder()
                                       .transferId(transferId)
                                       .initialState(TransferState.DENIED)
                                       .message(policyResult.getReason())
                                       .success(false)
                                       .build();
            }

            // Step 4: Policies approved
            transfer.setCurrentState(TransferState.APPROVED);
            transfer.setMessage("All policies satisfied");
            // Final save at transaction commit

            auditService.logStateTransition(transferId, TransferState.POLICY_EVALUATION,
                TransferState.APPROVED, "All policies satisfied");

            // Step 5: Initiate async transfer workflow
            executeTransferWorkflowAsync(transferId, request);

            log.info("Transfer approved and workflow started: {}", transferId);

            return TransferResponse.builder()
                                   .transferId(transferId)
                                   .initialState(TransferState.APPROVED)
                                   .message("Transfer approved, processing started")
                                   .success(true)
                                   .build();

        } catch (Exception e) {
            log.error("Error initiating transfer", e);

            return TransferResponse.builder()
                                   .transferId(null)
                                   .initialState(TransferState.FAILED)
                                   .message("Internal error: " + e.getMessage())
                                   .success(false)
                                   .build();
        }
    }

    /**
     * Gets current status of a transfer
     */
    @Transactional(readOnly = true)
    public TransferStatus getTransferStatus(String transferId) {
        log.debug("Getting transfer status: {}", transferId);

        TransferEntity transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        return TransferStatus.builder()
            .transferId(transfer.getId())
            .currentState(transfer.getCurrentState())
            .message(transfer.getMessage())
            .lastUpdated(transfer.getLastUpdated())
            .retryCount(transfer.getRetryCount())
            .edcTransferProcessId(transfer.getEdcTransferProcessId())
            .build();
    }

    /**
     * Cancels an in-progress transfer
     */
    @Transactional
    public void cancelTransfer(String transferId) {
        log.info("Cancelling transfer: {}", transferId);

        TransferEntity transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        TransferState previousState = transfer.getCurrentState();

        // Terminate EDC transfer if exists
        if (transfer.getEdcTransferProcessId() != null) {
            try {
                edcConnectorClient.terminateTransfer(transfer.getEdcTransferProcessId());
            } catch (Exception e) {
                log.error("Error terminating EDC transfer: {}", transferId, e);
            }
        }

        transfer.setCurrentState(TransferState.CANCELLED);
        transfer.setMessage("Transfer cancelled by user");
        transferRepository.save(transfer);

        auditService.logStateTransition(transferId, previousState,
            TransferState.CANCELLED, "Transfer cancelled by user");

        log.info("Transfer cancelled successfully: {}", transferId);
    }

    /**
     * Retrieves audit log for a transfer
     */
    public List<AuditEvent> getTransferAuditLog(String transferId) {
        log.debug("Getting audit log for transfer: {}", transferId);
        return auditService.getAuditTrail(transferId);
    }

    /**
     * Gets paginated list of transfers
     */
    @Transactional(readOnly = true)
    public Page<TransferStatus> listTransfers(Pageable pageable) {
        log.debug("Listing transfers with pagination: {}", pageable);

        return transferRepository.findAll(pageable)
            .map(this::toTransferStatus);
    }

    /**
     * Async method to execute transfer workflow
     */
    @Async("taskExecutor")
    public void executeTransferWorkflowAsync(String transferId, TransferRequest request) {
        log.info("Starting async transfer workflow: {}", transferId);

        try {
            executeTransferWorkflow(transferId, request, 0);
        } catch (Exception e) {
            log.error("Fatal error in transfer workflow: {}", transferId, e);
            markTransferAsFailed(transferId, "Workflow execution error: " + e.getMessage());
        }
    }

    /**
     * Executes the transfer workflow with retry logic
     */
    @Transactional
    protected void executeTransferWorkflow(String transferId, TransferRequest request, int retryCount) {
        try {
            TransferEntity transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

            // Step 1: Contract Negotiation
            log.debug("Starting contract negotiation for transfer: {}", transferId);
            transfer.setCurrentState(TransferState.CONTRACT_NEGOTIATION);

            auditService.logStateTransition(transferId, TransferState.APPROVED,
                TransferState.CONTRACT_NEGOTIATION, "Starting contract negotiation");

            ContractOffer offer = ContractOffer.builder()
                .assetId(request.getAssetId())
                .providerId(request.getProviderId())
                .consumerId(request.getConsumerId())
                .policyId("default-policy")
                .build();

            ContractNegotiationResult negotiationResult = edcConnectorClient.negotiateContract(offer);

            if (!negotiationResult.isSuccess()) {
                throw new RuntimeException("Contract negotiation failed: " + negotiationResult.getMessage());
            }

            transfer.setEdcContractAgreementId(negotiationResult.getAgreementId());
            transfer.setCurrentState(TransferState.NEGOTIATED);

            auditService.logStateTransition(transferId, TransferState.CONTRACT_NEGOTIATION,
                TransferState.NEGOTIATED, "Contract negotiated successfully");

            // Step 2: Initiate Transfer
            log.debug("Initiating EDC transfer for: {}", transferId);
            transfer.setCurrentState(TransferState.TRANSFER_IN_PROGRESS);

            auditService.logStateTransition(transferId, TransferState.NEGOTIATED,
                TransferState.TRANSFER_IN_PROGRESS, "Starting data transfer");

            TransferProcessResult processResult = edcConnectorClient.initiateTransfer(
                negotiationResult.getAgreementId(), request);

            if (!processResult.isSuccess()) {
                throw new RuntimeException("Transfer initiation failed: " + processResult.getMessage());
            }

            transfer.setEdcTransferProcessId(processResult.getTransferProcessId());

            // Step 3: Monitor Transfer (simplified - in production would poll EDC)
            Thread.sleep(2000); // Simulate transfer time

            TransferProcessState edcState = edcConnectorClient.getTransferState(
                processResult.getTransferProcessId());

            if (edcState == TransferProcessState.COMPLETED) {
                transfer.setCurrentState(TransferState.COMPLETED);
                transfer.setMessage("Transfer completed successfully");

                auditService.logTransferCompletion(transferId, TransferState.COMPLETED,
                    "Transfer completed successfully");

                log.info("Transfer completed successfully: {}", transferId);

            } else if (edcState == TransferProcessState.ERROR) {
                throw new RuntimeException("EDC transfer failed");
            }

        } catch (Exception e) {
            log.error("Error in transfer workflow (attempt {}): {}", retryCount + 1, transferId, e);

            if (retryCount < MAX_RETRY_ATTEMPTS) {
                handleRetry(transferId, request, retryCount, e.getMessage());
            } else {
                markTransferAsFailed(transferId, "Max retry attempts exceeded: " + e.getMessage());
            }
        }
    }

    /**
     * Handles retry with exponential backoff
     */
    @Transactional
    protected void handleRetry(String transferId, TransferRequest request, int retryCount, String reason) {
        int nextRetry = retryCount + 1;
        long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount);

        log.info("Scheduling retry {} for transfer {} after {}ms", nextRetry, transferId, backoffMs);

        transferRepository.findById(transferId).ifPresent(transfer ->
            transfer.setRetryCount(nextRetry)
        );

        auditService.logRetryAttempt(transferId, nextRetry, reason);

        try {
            TimeUnit.MILLISECONDS.sleep(backoffMs);
            executeTransferWorkflow(transferId, request, nextRetry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markTransferAsFailed(transferId, "Retry interrupted");
        }
    }

    /**
     * Marks transfer as failed
     */
    @Transactional
    protected void markTransferAsFailed(String transferId, String reason) {
        log.error("Marking transfer as failed: {} - {}", transferId, reason);

        TransferEntity transfer = transferRepository.findById(transferId).orElse(null);
        if (transfer != null) {
            TransferState previousState = transfer.getCurrentState();
            transfer.setCurrentState(TransferState.FAILED);
            transfer.setMessage(reason);
            transferRepository.save(transfer);

            auditService.logStateTransition(transferId, previousState,
                TransferState.FAILED, reason);
            auditService.logTransferCompletion(transferId, TransferState.FAILED, reason);
        }
    }

    private TransferEntity createTransferEntity(TransferRequest request) {
        return TransferEntity.builder()
            // .id() - Don't set ID, let JPA generate it
            .consumerId(request.getConsumerId())
            .providerId(request.getProviderId())
            .assetId(request.getAssetId())
            .dataType(request.getDataType())
            .currentState(TransferState.REQUESTED)
            .message("Transfer requested")
            .retryCount(0)
            .consumerRegion(request.getConsumerRegion())
            .consumerCertificationLevel(request.getConsumerCertificationLevel())
            .usagePurpose(request.getUsagePurpose())
            .createdAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private TransferStatus toTransferStatus(TransferEntity entity) {
        return TransferStatus.builder()
            .transferId(entity.getId())
            .currentState(entity.getCurrentState())
            .message(entity.getMessage())
            .lastUpdated(entity.getLastUpdated())
            .retryCount(entity.getRetryCount())
            .edcTransferProcessId(entity.getEdcTransferProcessId())
            .build();
    }
}

