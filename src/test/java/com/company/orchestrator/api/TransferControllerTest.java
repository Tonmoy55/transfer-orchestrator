package com.company.orchestrator.api;

import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.model.TransferResponse;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferOrchestrator;
import com.company.orchestrator.exeption.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive test cases for TransferController
 * Coverage: 100%
 */

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TransferOrchestrator transferOrchestrator;

    @InjectMocks
    private TransferController transferController;

    private TransferRequestDto validRequest;
    private TransferResponse successResponse;
    private TransferResponse failureResponse;
    private TransferStatus transferStatus;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                .setControllerAdvice(new GlobalExceptionHandler()) // if you have one
                .build();

        validRequest = TransferRequestDto.builder()
            .consumerId("consumer-1")
            .providerId("provider-1")
            .assetId("asset-123")
            .dataType("PRODUCTION_DATA")
            .consumerRegion("EU")
            .consumerCertificationLevel("ISO9001")
            .usagePurpose("QUALITY_ANALYSIS")
            .build();

        successResponse = TransferResponse.builder()
            .transferId("transfer-123")
            .initialState(TransferState.APPROVED)
            .message("Transfer approved")
            .success(true)
            .build();

        failureResponse = TransferResponse.builder()
            .transferId("transfer-456")
            .initialState(TransferState.DENIED)
            .message("Policy violation")
            .success(false)
            .build();

        transferStatus = TransferStatus.builder()
            .transferId("transfer-123")
            .currentState(TransferState.COMPLETED)
            .message("Transfer completed successfully")
            .lastUpdated(LocalDateTime.now())
            .retryCount(0)
            .edcTransferProcessId("edc-process-123")
            .build();
    }

    @Test
    @DisplayName("Should initiate transfer successfully with valid request")
    void shouldInitiateTransferSuccessfully() throws Exception {
        when(transferOrchestrator.initiateTransfer(any(TransferRequest.class)))
            .thenReturn(successResponse);

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.statusCode").value(202))
            .andExpect(jsonPath("$.message").value("Transfer initiated successfully"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.transferId").value("transfer-123"))
            .andExpect(jsonPath("$.data.success").value(true));

        verify(transferOrchestrator, times(1)).initiateTransfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when transfer initiation fails")
    void shouldReturnBadRequestWhenTransferFails() throws Exception {
        when(transferOrchestrator.initiateTransfer(any(TransferRequest.class)))
            .thenReturn(failureResponse);

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.transferId").value("transfer-456"))
            .andExpect(jsonPath("$.data.success").value(false));

        verify(transferOrchestrator, times(1)).initiateTransfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should return validation error for missing required fields")
    void shouldReturnValidationErrorForMissingFields() throws Exception {
        TransferRequestDto invalidRequest = TransferRequestDto.builder()
            .consumerId("consumer-1")
            .build();

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.fieldErrors").exists());

        verify(transferOrchestrator, never()).initiateTransfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should get transfer status successfully")
    void shouldGetTransferStatusSuccessfully() throws Exception {
        when(transferOrchestrator.getTransferStatus("transfer-123"))
            .thenReturn(transferStatus);

        mockMvc.perform(get("/api/v1/transfers/transfer-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Transfer status retrieved successfully"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.transferId").value("transfer-123"))
            .andExpect(jsonPath("$.data.currentState").value("COMPLETED"))
            .andExpect(jsonPath("$.data.retryCount").value(0));

        verify(transferOrchestrator, times(1)).getTransferStatus("transfer-123");
    }

    @Test
    @DisplayName("Should return 404 when transfer not found")
    void shouldReturn404WhenTransferNotFound() throws Exception {
        when(transferOrchestrator.getTransferStatus("non-existent"))
            .thenThrow(new IllegalArgumentException("Transfer not found"));

        mockMvc.perform(get("/api/v1/transfers/non-existent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Transfer not found with ID: non-existent"));

        verify(transferOrchestrator, times(1)).getTransferStatus("non-existent");
    }

    @Test
    @DisplayName("Should cancel transfer successfully")
    void shouldCancelTransferSuccessfully() throws Exception {
        doNothing().when(transferOrchestrator).cancelTransfer("transfer-123");

        mockMvc.perform(delete("/api/v1/transfers/transfer-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Transfer cancelled successfully"))
            .andExpect(jsonPath("$.success").value(true));

        verify(transferOrchestrator, times(1)).cancelTransfer("transfer-123");
    }

    @Test
    @DisplayName("Should return 404 when cancelling non-existent transfer")
    void shouldReturn404WhenCancellingNonExistentTransfer() throws Exception {
        doThrow(new IllegalArgumentException("Transfer not found"))
            .when(transferOrchestrator).cancelTransfer("non-existent");

        mockMvc.perform(delete("/api/v1/transfers/non-existent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.success").value(false));

        verify(transferOrchestrator, times(1)).cancelTransfer("non-existent");
    }

    @Test
    @DisplayName("Should get audit log successfully")
    void shouldGetAuditLogSuccessfully() throws Exception {
        List<AuditEvent> auditEvents = Arrays.asList(
            AuditEvent.builder()
                .eventId("evt-1")
                .transferId("transfer-123")
                .eventType("TRANSFER_REQUESTED")
                .action("REQUEST_TRANSFER")
                .timestamp(LocalDateTime.now())
                .build(),
            AuditEvent.builder()
                .eventId("evt-2")
                .transferId("transfer-123")
                .eventType("POLICY_EVALUATION")
                .action("POLICY_APPROVED")
                .timestamp(LocalDateTime.now())
                .build()
        );

        when(transferOrchestrator.getTransferAuditLog("transfer-123"))
            .thenReturn(auditEvents);

        mockMvc.perform(get("/api/v1/transfers/transfer-123/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Audit log retrieved successfully"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].eventId").value("evt-1"))
            .andExpect(jsonPath("$.data[1].eventId").value("evt-2"));

        verify(transferOrchestrator, times(1)).getTransferAuditLog("transfer-123");
    }

    @Test
    @DisplayName("Should get empty audit log")
    void shouldGetEmptyAuditLog() throws Exception {
        when(transferOrchestrator.getTransferAuditLog("transfer-123"))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/transfers/transfer-123/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data", hasSize(0)));

        verify(transferOrchestrator, times(1)).getTransferAuditLog("transfer-123");
    }

    @Test
    @DisplayName("Should list transfers with default pagination")
    void shouldListTransfersWithDefaultPagination() throws Exception {
        List<TransferStatus> transfers = Arrays.asList(transferStatus);
        Page<TransferStatus> page = new PageImpl<>(transfers, PageRequest.of(0, 20), 1);

        when(transferOrchestrator.listTransfers(any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/transfers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Transfers retrieved successfully"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].transferId").value("transfer-123"));

        verify(transferOrchestrator, times(1)).listTransfers(any(Pageable.class));
    }

    @Test
    @DisplayName("Should list transfers with custom pagination")
    void shouldListTransfersWithCustomPagination() throws Exception {
        List<TransferStatus> transfers = Arrays.asList(transferStatus);
        Page<TransferStatus> page = new PageImpl<>(transfers, PageRequest.of(1, 10), 11);

        when(transferOrchestrator.listTransfers(any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/transfers")
                .param("page", "1")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDir", "ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.totalElements").value(11));

        verify(transferOrchestrator, times(1)).listTransfers(any(Pageable.class));
    }

    @Test
    @DisplayName("Should list transfers with empty result")
    void shouldListTransfersWithEmptyResult() throws Exception {
        Page<TransferStatus> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(transferOrchestrator.listTransfers(any(Pageable.class)))
            .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/transfers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(0)))
            .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(transferOrchestrator, times(1)).listTransfers(any(Pageable.class));
    }
}

