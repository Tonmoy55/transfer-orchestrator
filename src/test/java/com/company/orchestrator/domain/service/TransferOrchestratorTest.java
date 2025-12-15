package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.*;
import com.company.orchestrator.exeption.CancelTransferException;
import com.company.orchestrator.infrastructure.edc.EdcConnectorClient;
import com.company.orchestrator.infrastructure.persistence.entity.TransferEntity;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransferOrchestrator}.
 *
 * These tests focus only on the public API:
 * - initiateTransfer
 * - getTransferStatus
 * - cancelTransfer
 * - getTransferAuditLog
 * - listTransfers
 *
 * Internal workflow/virtual-thread logic is *not* exercised here; that belongs in integration tests.
 */
@ExtendWith(MockitoExtension.class)
class TransferOrchestratorTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private PolicyEvaluationService policyEvaluationService;

    @Mock
    private AuditService auditService;

    @Mock
    private EdcConnectorClient edcConnectorClient;

    @InjectMocks
    private TransferOrchestrator transferOrchestrator;

    private TransferRequest transferRequest;
    private TransferEntity persistedEntity;
    private PolicyEvaluationResult approvedPolicies;
    private PolicyEvaluationResult deniedPolicies;

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

        persistedEntity = TransferEntity.builder()
                .id("transfer-123")
                .consumerId("consumer-1")
                .providerId("provider-1")
                .assetId("asset-123")
                .dataType("PRODUCTION_DATA")
                .currentState(TransferState.REQUESTED)
                .message("Transfer requested")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();

        approvedPolicies = PolicyEvaluationResult.builder()
                .allowed(true)
                .reason("All policies satisfied")
                .build();

        deniedPolicies = PolicyEvaluationResult.builder()
                .allowed(false)
                .reason("Policy violation")
                .build();

        // Common: avoid real side-effects from audit service
        lenient().doNothing().when(auditService).logTransferRequest(any());
        lenient().doNothing().when(auditService).logPolicyEvaluation(anyString(), any());
        lenient().doNothing().when(auditService).logStateTransition(anyString(), any(), any(), anyString());
        lenient().doNothing().when(auditService).logTransferCompletion(anyString(), any(), anyString());
        lenient().doNothing().when(auditService).logRetryAttempt(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("initiateTransfer: returns APPROVED and starts workflow when policies pass")
    void initiateTransfer_policiesApproved_startsWorkflow() {
        // Arrange
        when(transferRepository.save(any(TransferEntity.class))).thenReturn(persistedEntity);
        when(policyEvaluationService.evaluateAll(any(TransferRequest.class))).thenReturn(approvedPolicies);

        // We don't want to actually start a virtual thread in the unit test; just verify it's invoked.
        TransferOrchestrator spyOrchestrator = spy(transferOrchestrator);
        doNothing().when(spyOrchestrator).executeTransferWorkflowAsync(anyString(), any(TransferRequest.class));

        // Act
        TransferResponse response = spyOrchestrator.initiateTransfer(transferRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("transfer-123", response.getTransferId());
        assertEquals(TransferState.APPROVED, response.getInitialState());
        assertEquals("Transfer approved, processing started", response.getMessage());

        // Entity must be saved once
        verify(transferRepository, times(1)).save(any(TransferEntity.class));
        // Policies evaluated
        verify(policyEvaluationService, times(1)).evaluateAll(any(TransferRequest.class));
        // Audit calls
        verify(auditService).logTransferRequest(any(TransferRequest.class));
        verify(auditService).logPolicyEvaluation(eq("transfer-123"), eq(approvedPolicies));
        // Workflow started asynchronously
        verify(spyOrchestrator).executeTransferWorkflowAsync(eq("transfer-123"), any(TransferRequest.class));
    }

    @Test
    @DisplayName("initiateTransfer: returns DENIED and does NOT start workflow when policies fail")
    void initiateTransfer_policiesDenied_returnsDenied() {
        // Arrange
        when(transferRepository.save(any(TransferEntity.class))).thenReturn(persistedEntity);
        when(policyEvaluationService.evaluateAll(any(TransferRequest.class))).thenReturn(deniedPolicies);

        // No need to stub executeTransferWorkflowAsync here since it must never be called
        TransferOrchestrator spyOrchestrator = spy(transferOrchestrator);

        // Act
        TransferResponse response = spyOrchestrator.initiateTransfer(transferRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("transfer-123", response.getTransferId());
        assertEquals(TransferState.DENIED, response.getInitialState());
        assertEquals("Policy violation", response.getMessage());

        // Workflow must NOT be started
        verify(spyOrchestrator, never()).executeTransferWorkflowAsync(anyString(), any(TransferRequest.class));

        // Policy evaluation + audit
        verify(policyEvaluationService, times(1)).evaluateAll(any(TransferRequest.class));
        verify(auditService).logPolicyEvaluation(eq("transfer-123"), eq(deniedPolicies));
    }

    @Test
    @DisplayName("initiateTransfer: returns FAILED when an unexpected exception occurs")
    void initiateTransfer_unexpectedException_returnsFailed() {
        // Arrange
        when(transferRepository.save(any(TransferEntity.class))).thenThrow(new RuntimeException("DB down"));

        // Act
        TransferResponse response = transferOrchestrator.initiateTransfer(transferRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertNull(response.getTransferId());
        assertEquals(TransferState.FAILED, response.getInitialState());
        assertTrue(response.getMessage().startsWith("Internal error:"));
    }

    @Test
    @DisplayName("getTransferStatus: returns status when transfer exists")
    void getTransferStatus_existingTransfer_returnsStatus() {
        // Arrange
        persistedEntity.setCurrentState(TransferState.TRANSFER_IN_PROGRESS);
        persistedEntity.setMessage("In progress");
        persistedEntity.setRetryCount(2);
        persistedEntity.setEdcTransferProcessId("edc-123");

        when(transferRepository.findById("transfer-123")).thenReturn(Optional.of(persistedEntity));

        // Act
        TransferStatus status = transferOrchestrator.getTransferStatus("transfer-123");

        // Assert
        assertNotNull(status);
        assertEquals("transfer-123", status.getTransferId());
        assertEquals(TransferState.TRANSFER_IN_PROGRESS, status.getCurrentState());
        assertEquals("In progress", status.getMessage());
        assertEquals(2, status.getRetryCount());
        assertEquals("edc-123", status.getEdcTransferProcessId());
    }

    @Test
    @DisplayName("getTransferStatus: throws IllegalArgumentException when transfer not found")
    void getTransferStatus_missingTransfer_throws() {
        when(transferRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> transferOrchestrator.getTransferStatus("missing"));
    }

    @Test
    @DisplayName("cancelTransfer: cancels non-completed transfer and logs state change")
    void cancelTransfer_nonCompleted_cancelsAndLogs() {
        // Arrange
        persistedEntity.setCurrentState(TransferState.TRANSFER_IN_PROGRESS);
        persistedEntity.setEdcTransferProcessId("edc-999");

        when(transferRepository.findById("transfer-123")).thenReturn(Optional.of(persistedEntity));
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(edcConnectorClient).terminateTransfer("edc-999");

        // Act
        transferOrchestrator.cancelTransfer("transfer-123");

        // Assert
        ArgumentCaptor<TransferEntity> captor = ArgumentCaptor.forClass(TransferEntity.class);
        verify(transferRepository).save(captor.capture());

        TransferEntity saved = captor.getValue();
        assertEquals(TransferState.CANCELLED, saved.getCurrentState());
        assertEquals("Transfer cancelled by user", saved.getMessage());

        verify(edcConnectorClient).terminateTransfer("edc-999");
        verify(auditService).logStateTransition(eq("transfer-123"), eq(TransferState.TRANSFER_IN_PROGRESS),
                eq(TransferState.CANCELLED), anyString());
    }

    @Test
    @DisplayName("cancelTransfer: throws CancelTransferException for COMPLETED transfer")
    void cancelTransfer_completed_throwsCancelTransferException() {
        persistedEntity.setCurrentState(TransferState.COMPLETED);
        when(transferRepository.findById("transfer-123")).thenReturn(Optional.of(persistedEntity));

        assertThrows(CancelTransferException.class,
                () -> transferOrchestrator.cancelTransfer("transfer-123"));

        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelTransfer: propagates IllegalArgumentException when transfer ID not found")
    void cancelTransfer_missingTransfer_throwsIllegalArgumentException() {
        when(transferRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> transferOrchestrator.cancelTransfer("missing"));
    }

    @Test
    @DisplayName("cancelTransfer: logs error but still cancels when EDC termination fails")
    void cancelTransfer_edcTerminationFails_stillCancels() {
        persistedEntity.setCurrentState(TransferState.TRANSFER_IN_PROGRESS);
        persistedEntity.setEdcTransferProcessId("edc-999");

        when(transferRepository.findById("transfer-123")).thenReturn(Optional.of(persistedEntity));
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("EDC error"))
                .when(edcConnectorClient).terminateTransfer("edc-999");

        // Act (should not throw)
        assertDoesNotThrow(() -> transferOrchestrator.cancelTransfer("transfer-123"));

        ArgumentCaptor<TransferEntity> captor = ArgumentCaptor.forClass(TransferEntity.class);
        verify(transferRepository).save(captor.capture());
        assertEquals(TransferState.CANCELLED, captor.getValue().getCurrentState());
    }


    @Test
    @DisplayName("getTransferAuditLog: delegates to AuditService")
    void getTransferAuditLog_delegatesToAuditService() {
        List<AuditEvent> events = List.of(
                AuditEvent.builder().eventId("e1").build(),
                AuditEvent.builder().eventId("e2").build()
        );

        when(auditService.getAuditTrail("transfer-123")).thenReturn(events);

        List<AuditEvent> result = transferOrchestrator.getTransferAuditLog("transfer-123");

        assertEquals(2, result.size());
        assertEquals("e1", result.get(0).getEventId());
        verify(auditService).getAuditTrail("transfer-123");
    }

    @Test
    @DisplayName("listTransfers: maps entities to TransferStatus page")
    void listTransfers_mapsEntitiesToStatus() {
        persistedEntity.setCurrentState(TransferState.COMPLETED);
        persistedEntity.setMessage("done");
        persistedEntity.setRetryCount(1);
        persistedEntity.setEdcTransferProcessId("edc-1");

        Pageable pageable = PageRequest.of(0, 5);
        Page<TransferEntity> entityPage = new PageImpl<>(Collections.singletonList(persistedEntity), pageable, 1);

        when(transferRepository.findAll(pageable)).thenReturn(entityPage);

        Page<TransferStatus> result = transferOrchestrator.listTransfers(pageable);

        assertEquals(1, result.getTotalElements());
        TransferStatus status = result.getContent().get(0);
        assertEquals("transfer-123", status.getTransferId());
        assertEquals(TransferState.COMPLETED, status.getCurrentState());
        assertEquals("done", status.getMessage());
        assertEquals("edc-1", status.getEdcTransferProcessId());
        assertEquals(1, status.getRetryCount());
    }
}
