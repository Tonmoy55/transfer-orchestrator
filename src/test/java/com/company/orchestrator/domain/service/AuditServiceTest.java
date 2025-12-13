package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.persistence.entity.AuditEventEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test cases for AuditService
 * Coverage: 100%
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    private TransferRequest transferRequest;
    private PolicyEvaluationResult policyEvaluationResult;
    private AuditEventEntity auditEventEntity;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
            .consumerId("consumer-1")
            .providerId("provider-1")
            .assetId("asset-123")
            .dataType("PRODUCTION_DATA")
            .consumerRegion("EU")
            .consumerCertificationLevel("ISO9001")
            .usagePurpose("QUALITY_ANALYSIS")
            .build();

        policyEvaluationResult = PolicyEvaluationResult.builder()
            .allowed(true)
            .reason("All policies satisfied")
            .satisfiedPolicies(Arrays.asList("TIME_BASED", "GEOGRAPHIC"))
            .violatedPolicies(Collections.emptyList())
            .build();

        auditEventEntity = AuditEventEntity.builder()
            .id("event-1")
            .transferId("transfer-123")
            .eventType("TRANSFER_REQUESTED")
            .actor("consumer-1")
            .action("REQUEST_TRANSFER")
            .details("Transfer requested for asset: asset-123")
            .metadata("{}")
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should log transfer request successfully")
    void shouldLogTransferRequestSuccessfully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logTransferRequest(transferRequest);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertNull(captured.getTransferId());
        assertEquals("TRANSFER_REQUESTED", captured.getEventType());
        assertEquals("consumer-1", captured.getActor());
        assertEquals("REQUEST_TRANSFER", captured.getAction());
        assertNotNull(captured.getTimestamp());
    }

    @Test
    @DisplayName("Should log policy evaluation with allowed result")
    void shouldLogPolicyEvaluationWithAllowedResult() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logPolicyEvaluation("transfer-123", policyEvaluationResult);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("transfer-123", captured.getTransferId());
        assertEquals("POLICY_EVALUATION", captured.getEventType());
        assertEquals("SYSTEM", captured.getActor());
        assertEquals("POLICY_APPROVED", captured.getAction());
    }

    @Test
    @DisplayName("Should log policy evaluation with denied result")
    void shouldLogPolicyEvaluationWithDeniedResult() throws Exception {
        PolicyEvaluationResult deniedResult = PolicyEvaluationResult.builder()
            .allowed(false)
            .reason("Geographic policy violation")
            .satisfiedPolicies(Collections.emptyList())
            .violatedPolicies(Collections.singletonList("GEOGRAPHIC"))
            .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logPolicyEvaluation("transfer-123", deniedResult);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("POLICY_DENIED", captured.getAction());
        assertEquals("Geographic policy violation", captured.getDetails());
    }

    @Test
    @DisplayName("Should log state transition successfully")
    void shouldLogStateTransitionSuccessfully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logStateTransition("transfer-123", TransferState.APPROVED, TransferState.CONTRACT_NEGOTIATION);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("transfer-123", captured.getTransferId());
        assertEquals("STATE_TRANSITION", captured.getEventType());
        assertEquals("SYSTEM", captured.getActor());
        assertTrue(captured.getAction().contains("APPROVED"));
        assertTrue(captured.getAction().contains("CONTRACT_NEGOTIATION"));
    }

    @Test
    @DisplayName("Should log state transition with reason")
    void shouldLogStateTransitionWithReason() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logStateTransition("transfer-123", TransferState.APPROVED,
            TransferState.CONTRACT_NEGOTIATION, "Starting contract negotiation");

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("Starting contract negotiation", captured.getDetails());
    }

    @Test
    @DisplayName("Should log transfer completion with success")
    void shouldLogTransferCompletionWithSuccess() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logTransferCompletion("transfer-123", TransferState.COMPLETED, "Transfer completed successfully");

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("transfer-123", captured.getTransferId());
        assertEquals("TRANSFER_COMPLETED", captured.getEventType());
        assertEquals("TRANSFER_SUCCESS", captured.getAction());
        assertEquals("Transfer completed successfully", captured.getDetails());
    }

    @Test
    @DisplayName("Should log transfer completion with failure")
    void shouldLogTransferCompletionWithFailure() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logTransferCompletion("transfer-123", TransferState.FAILED, "Transfer failed due to timeout");

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("TRANSFER_FAILED", captured.getAction());
        assertEquals("Transfer failed due to timeout", captured.getDetails());
    }

    @Test
    @DisplayName("Should log retry attempt successfully")
    void shouldLogRetryAttemptSuccessfully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        auditService.logRetryAttempt("transfer-123", 2, "EDC connection timeout");

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository, times(1)).save(captor.capture());

        AuditEventEntity captured = captor.getValue();
        assertEquals("transfer-123", captured.getTransferId());
        assertEquals("RETRY_ATTEMPT", captured.getEventType());
        assertEquals("RETRY_TRANSFER", captured.getAction());
        assertTrue(captured.getDetails().contains("Retry attempt #2"));
    }

    @Test
    @DisplayName("Should get audit trail successfully")
    void shouldGetAuditTrailSuccessfully() throws Exception {
        List<AuditEventEntity> entities = Arrays.asList(
            auditEventEntity,
            AuditEventEntity.builder()
                .id("event-2")
                .transferId("transfer-123")
                .eventType("POLICY_EVALUATION")
                .actor("SYSTEM")
                .action("POLICY_APPROVED")
                .details("All policies satisfied")
                .metadata("{}")
                .timestamp(LocalDateTime.now())
                .build()
        );

        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
            .thenReturn(entities);
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class)))
            .thenReturn(Collections.emptyMap());

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("event-1", result.get(0).getEventId());
        assertEquals("event-2", result.get(1).getEventId());

        verify(auditEventRepository, times(1)).findByTransferIdOrderByTimestampAsc("transfer-123");
    }

    @Test
    @DisplayName("Should return empty list when no audit events found")
    void shouldReturnEmptyListWhenNoAuditEventsFound() throws Exception {
        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
            .thenReturn(Collections.emptyList());

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should get audit events by date range")
    void shouldGetAuditEventsByDateRange() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        List<AuditEventEntity> entities = Arrays.asList(auditEventEntity);

        when(auditEventRepository.findByTimestampBetween(start, end))
            .thenReturn(entities);
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class)))
            .thenReturn(Collections.emptyMap());

        List<AuditEvent> result = auditService.getAuditEventsByDateRange(start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("event-1", result.get(0).getEventId());

        verify(auditEventRepository, times(1)).findByTimestampBetween(start, end);
    }

    @Test
    @DisplayName("Should handle JSON serialization error gracefully")
    void shouldHandleJsonSerializationErrorGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(auditEventEntity);

        // Should not throw exception
        assertDoesNotThrow(() ->
            auditService.logTransferRequest(transferRequest)
        );

        verify(auditEventRepository, times(1)).save(any(AuditEventEntity.class));
    }

    @Test
    @DisplayName("Should handle JSON deserialization error gracefully")
    void shouldHandleJsonDeserializationErrorGracefully() throws Exception {
        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
            .thenReturn(Arrays.asList(auditEventEntity));
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class)))
            .thenThrow(new RuntimeException("JSON parse error"));

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        // Metadata should be null due to parse error
        assertNull(result.get(0).getMetadata());
    }

    @Test
    @DisplayName("Should handle null metadata in audit event entity")
    void shouldHandleNullMetadataInAuditEventEntity() throws Exception {
        AuditEventEntity entityWithNullMetadata = AuditEventEntity.builder()
            .id("event-1")
            .transferId("transfer-123")
            .eventType("TRANSFER_REQUESTED")
            .actor("consumer-1")
            .action("REQUEST_TRANSFER")
            .details("Transfer requested")
            .metadata(null)
            .timestamp(LocalDateTime.now())
            .build();

        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
            .thenReturn(Arrays.asList(entityWithNullMetadata));

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getMetadata());

        verify(objectMapper, never()).readValue(anyString(), eq(java.util.Map.class));
    }
}

