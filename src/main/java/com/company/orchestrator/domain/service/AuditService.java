package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.infrastructure.persistence.entity.AuditEventEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for comprehensive audit logging
 * Critical Component: Immutable, append-only audit trail
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Logs transfer request initiation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransferRequest(String transferId, TransferRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("consumerId", request.getConsumerId());
        metadata.put("providerId", request.getProviderId());
        metadata.put("assetId", request.getAssetId());
        metadata.put("dataType", request.getDataType());

        AuditEventEntity event = AuditEventEntity.builder()
            .transferId(transferId)
            .eventType("TRANSFER_REQUESTED")
            .actor(request.getConsumerId())
            .action("REQUEST_TRANSFER")
            .details(String.format("Transfer requested for asset: %s", request.getAssetId()))
            .metadata(serializeMetadata(metadata))
            .timestamp(LocalDateTime.now())
            .build();

        auditEventRepository.save(event);
        log.debug("Audit: Transfer requested - ID: {}", transferId);
    }

    /**
     * Logs policy evaluation result
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPolicyEvaluation(String transferId, PolicyEvaluationResult result) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("allowed", result.isAllowed());
        metadata.put("violatedPolicies", result.getViolatedPolicies());
        metadata.put("satisfiedPolicies", result.getSatisfiedPolicies());

        AuditEventEntity event = AuditEventEntity.builder()
            .transferId(transferId)
            .eventType("POLICY_EVALUATION")
            .actor("SYSTEM")
            .action(result.isAllowed() ? "POLICY_APPROVED" : "POLICY_DENIED")
            .details(result.getReason())
            .metadata(serializeMetadata(metadata))
            .timestamp(LocalDateTime.now())
            .build();

        auditEventRepository.save(event);
        log.debug("Audit: Policy evaluation - ID: {}, Result: {}",
            transferId, result.isAllowed() ? "APPROVED" : "DENIED");
    }

    /**
     * Logs state transition
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStateTransition(String transferId, TransferState from, TransferState to, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fromState", from);
        metadata.put("toState", to);

        AuditEventEntity event = AuditEventEntity.builder()
            .transferId(transferId)
            .eventType("STATE_TRANSITION")
            .actor("SYSTEM")
            .action(String.format("TRANSITION_%s_TO_%s", from, to))
            .details(reason != null ? reason : String.format("State changed from %s to %s", from, to))
            .metadata(serializeMetadata(metadata))
            .timestamp(LocalDateTime.now())
            .build();

        auditEventRepository.save(event);
        log.debug("Audit: State transition - ID: {}, {} -> {}", transferId, from, to);
    }

    /**
     * Logs transfer completion
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransferCompletion(String transferId, TransferState finalState, String message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("finalState", finalState);
        metadata.put("success", finalState == TransferState.COMPLETED);

        AuditEventEntity event = AuditEventEntity.builder()
            .transferId(transferId)
            .eventType("TRANSFER_COMPLETED")
            .actor("SYSTEM")
            .action(finalState == TransferState.COMPLETED ? "TRANSFER_SUCCESS" : "TRANSFER_FAILED")
            .details(message)
            .metadata(serializeMetadata(metadata))
            .timestamp(LocalDateTime.now())
            .build();

        auditEventRepository.save(event);
        log.info("Audit: Transfer completed - ID: {}, State: {}", transferId, finalState);
    }

    /**
     * Logs retry attempts
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRetryAttempt(String transferId, int retryCount, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("retryCount", retryCount);

        AuditEventEntity event = AuditEventEntity.builder()
            .transferId(transferId)
            .eventType("RETRY_ATTEMPT")
            .actor("SYSTEM")
            .action("RETRY_TRANSFER")
            .details(String.format("Retry attempt #%d: %s", retryCount, reason))
            .metadata(serializeMetadata(metadata))
            .timestamp(LocalDateTime.now())
            .build();

        auditEventRepository.save(event);
        log.debug("Audit: Retry attempt - ID: {}, Count: {}", transferId, retryCount);
    }

    /**
     * Retrieves audit trail for a specific transfer
     */
    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditTrail(String transferId) {
        log.debug("Retrieving audit trail for transfer: {}", transferId);

        return auditEventRepository.findByTransferIdOrderByTimestampAsc(transferId)
            .stream()
            .map(this::toAuditEvent)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves audit events within a date range
     */
    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Retrieving audit events between {} and {}", start, end);

        return auditEventRepository.findByTimestampBetween(start, end)
            .stream()
            .map(this::toAuditEvent)
            .collect(Collectors.toList());
    }

    private AuditEvent toAuditEvent(AuditEventEntity entity) {
        Map<String, Object> metadata = null;
        try {
            if (entity.getMetadata() != null) {
                metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata for audit event: {}", entity.getId());
        }

        return AuditEvent.builder()
            .eventId(entity.getId())
            .transferId(entity.getTransferId())
            .eventType(entity.getEventType())
            .actor(entity.getActor())
            .action(entity.getAction())
            .timestamp(entity.getTimestamp())
            .metadata(metadata)
            .details(entity.getDetails())
            .build();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit metadata", e);
            return "{}";
        }
    }
}

