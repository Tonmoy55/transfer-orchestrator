package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.persistence.entity.AuditEventEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test cases for AuditService
 * Coverage: 100%
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private ObjectMapper objectMapper;
    private AuditService auditService;

    private TransferRequest transferRequest;
    private PolicyEvaluationResult policyEvaluationResult;
    private AuditEventEntity auditEventEntity;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                                 .findAndAddModules()
                                 .build();

        auditService = new AuditService(auditEventRepository, objectMapper);

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
                                                       .satisfiedPolicies(List.of("TIME_BASED", "GEOGRAPHIC"))
                                                       .violatedPolicies(List.of())
                                                       .build();

        auditEventEntity = AuditEventEntity.builder()
                                           .id("event-1")
                                           .transferId("transfer-123")
                                           .eventType("TRANSFER_REQUESTED")
                                           .actor("consumer-1")
                                           .action("REQUEST_TRANSFER")
                                           .details("Transfer requested")
                                           .metadata("{}")
                                           .timestamp(LocalDateTime.now())
                                           .build();
    }

    @Test
    @DisplayName("Should log transfer request successfully")
    void shouldLogTransferRequestSuccessfully() {
        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logTransferRequest(transferRequest);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEventEntity saved = captor.getValue();
        assertEquals("TRANSFER_REQUESTED", saved.getEventType());
        assertEquals("consumer-1", saved.getActor());
        assertEquals("REQUEST_TRANSFER", saved.getAction());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    @DisplayName("Should log policy evaluation with allowed result")
    void shouldLogPolicyEvaluationWithAllowedResult() {
        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logPolicyEvaluation("transfer-123", policyEvaluationResult);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        assertEquals("POLICY_APPROVED", captor.getValue().getAction());
    }

    @Test
    @DisplayName("Should log policy evaluation with denied result")
    void shouldLogPolicyEvaluationWithDeniedResult() {
        PolicyEvaluationResult denied = PolicyEvaluationResult.builder()
                                                              .allowed(false)
                                                              .reason("Geographic violation")
                                                              .satisfiedPolicies(List.of())
                                                              .violatedPolicies(List.of("GEOGRAPHIC"))
                                                              .build();

        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logPolicyEvaluation("transfer-123", denied);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        assertEquals("POLICY_DENIED", captor.getValue().getAction());
        assertEquals("Geographic violation", captor.getValue().getDetails());
    }

    @Test
    @DisplayName("Should log state transition successfully")
    void shouldLogStateTransitionSuccessfully() {
        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logStateTransition(
                "transfer-123",
                TransferState.APPROVED,
                TransferState.CONTRACT_NEGOTIATION
        );

        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("Should log transfer completion successfully")
    void shouldLogTransferCompletionSuccessfully() {
        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logTransferCompletion(
                "transfer-123",
                TransferState.COMPLETED,
                "Completed"
        );

        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("Should log retry attempt successfully")
    void shouldLogRetryAttemptSuccessfully() {
        when(auditEventRepository.save(any())).thenReturn(auditEventEntity);

        auditService.logRetryAttempt("transfer-123", 2, "Timeout");

        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("Should get audit trail successfully")
    void shouldGetAuditTrailSuccessfully() {
        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
                .thenReturn(List.of(auditEventEntity));

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertEquals(1, result.size());
        assertEquals("event-1", result.get(0).getEventId());
    }

    @Test
    @DisplayName("Should return empty list when no audit events found")
    void shouldReturnEmptyListWhenNoAuditEventsFound() {
        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
                .thenReturn(List.of());

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle JSON serialization error gracefully")
    void shouldHandleJsonSerializationErrorGracefully() {
        AuditService faultyService =
                new AuditService(auditEventRepository, new ObjectMapper() {
                    @Override
                    public String writeValueAsString(Object value) {
                        throw new RuntimeException("JSON error");
                    }
                });

        assertDoesNotThrow(() -> faultyService.logTransferRequest(transferRequest));
    }

    @Test
    @DisplayName("Should handle null metadata in audit entity")
    void shouldHandleNullMetadataInAuditEventEntity() {
        AuditEventEntity entityWithNullMetadata = AuditEventEntity.builder()
                                                                  .metadata(null)
                                                                  .build();

        when(auditEventRepository.findByTransferIdOrderByTimestampAsc("transfer-123"))
                .thenReturn(List.of(entityWithNullMetadata));

        List<AuditEvent> result = auditService.getAuditTrail("transfer-123");

        assertNull(result.get(0).getMetadata());
    }
}

