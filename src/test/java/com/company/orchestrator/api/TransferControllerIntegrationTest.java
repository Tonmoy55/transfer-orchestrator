package com.company.orchestrator.api;

import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.model.TransferResponse;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferOrchestrator;
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

import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Transfer API
 */
@ExtendWith(MockitoExtension.class)
class TransferControllerIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    @Mock
    private TransferOrchestrator transferOrchestrator;
    @InjectMocks
    private TransferController transferController;
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                .build();
    }

    @Test
    @DisplayName("Should initiate transfer successfully with valid request")
    void shouldInitiateTransferSuccessfully() throws Exception {

        TransferRequestDto request = TransferRequestDto.builder()
                                                       .consumerId("consumer-1")
                                                       .providerId("provider-1")
                                                       .assetId("asset-123")
                                                       .dataType("PRODUCTION_DATA")
                                                       .consumerRegion("EU")
                                                       .consumerCertificationLevel("ISO9001")
                                                       .usagePurpose("QUALITY_ANALYSIS")
                                                       .build();

        TransferResponse mockResponse = TransferResponse.builder()
                                                        .transferId("transfer-123")
                                                        .success(true)
                                                        .build();

        when(transferOrchestrator.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transfers")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isAccepted())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.transferId").value("transfer-123"))
               .andExpect(jsonPath("$.data.success").value(true));

        verify(transferOrchestrator).initiateTransfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should deny transfer with invalid region")
    void shouldDenyTransferWithInvalidRegion() throws Exception {

        TransferRequestDto request = TransferRequestDto.builder()
                                                       .consumerId("consumer-1")
                                                       .providerId("provider-1")
                                                       .assetId("asset-123")
                                                       .dataType("PRODUCTION_DATA")
                                                       .consumerRegion("US") // invalid
                                                       .consumerCertificationLevel("ISO9001")
                                                       .usagePurpose("QUALITY_ANALYSIS")
                                                       .build();

        TransferResponse deniedResponse = TransferResponse.builder()
                                                          .transferId(null)
                                                          .success(false)
                                                          .message("Region not allowed")
                                                          .build();

        when(transferOrchestrator.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(deniedResponse);

        mockMvc.perform(post("/api/v1/transfers")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.data.success").value(false));

        verify(transferOrchestrator).initiateTransfer(any(TransferRequest.class));
    }


    @Test
    @DisplayName("Should return validation error for missing fields")
    void shouldReturnValidationErrorForMissingFields() throws Exception {
        TransferRequestDto request = TransferRequestDto.builder()
                                                       .consumerId("consumer-1")
                                                       .build();

        mockMvc.perform(post("/api/v1/transfers")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get transfer status")
    void shouldGetTransferStatus() throws Exception {

        TransferStatus status = TransferStatus.builder()
                                              .transferId("transfer-123")
                                              .currentState(TransferState.APPROVED)
                                              .message("Approved")
                                              .build();

        when(transferOrchestrator.getTransferStatus("transfer-123"))
                .thenReturn(status);

        mockMvc.perform(get("/api/v1/transfers/transfer-123"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.transferId").value("transfer-123"))
               .andExpect(jsonPath("$.data.currentState").value("APPROVED"));
    }


    @Test
    @DisplayName("Should return 404 for non-existent transfer")
    void shouldReturn404ForNonExistentTransfer() throws Exception {

        when(transferOrchestrator.getTransferStatus("non-existent-id"))
                .thenThrow(new IllegalArgumentException("Transfer not found"));

        mockMvc.perform(get("/api/v1/transfers/non-existent-id"))
               .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("Should list transfers with pagination")
    void shouldListTransfersWithPagination() throws Exception {

        List<TransferStatus> content = new ArrayList<>();
        content.add(
                TransferStatus.builder()
                              .transferId("t1")
                              .currentState(TransferState.APPROVED)
                              .build()
        );

        Page<TransferStatus> page =
                new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(transferOrchestrator.listTransfers(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/transfers")
                       .param("page", "0")
                       .param("size", "10"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.content").isArray());
    }



    @Test
    @DisplayName("Should get audit log for transfer")
    void shouldGetAuditLogForTransfer() throws Exception {

        List<AuditEvent> auditEvents = List.of(
                AuditEvent.builder()
                          .eventId("event-1")
                          .transferId("transfer-123")
                          .build()
        );

        when(transferOrchestrator.getTransferAuditLog("transfer-123"))
                .thenReturn(auditEvents);

        mockMvc.perform(get("/api/v1/transfers/transfer-123/audit"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data").isArray());
    }

}
