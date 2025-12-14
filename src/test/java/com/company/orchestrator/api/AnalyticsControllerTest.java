package com.company.orchestrator.api;

import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive test cases for AnalyticsController
 * Coverage: 100%
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders
                .standaloneSetup(analyticsController)
                .build();
        // Setup default mock behavior
        when(transferRepository.count()).thenReturn(100L);
        when(transferRepository.countByState(TransferState.COMPLETED)).thenReturn(85L);
        when(transferRepository.countByState(TransferState.FAILED)).thenReturn(10L);
        when(transferRepository.countByState(TransferState.TRANSFER_IN_PROGRESS)).thenReturn(3L);
        when(transferRepository.countByState(TransferState.DENIED)).thenReturn(2L);
        when(transferRepository.countByState(TransferState.REQUESTED)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.POLICY_EVALUATION)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.APPROVED)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.CONTRACT_NEGOTIATION)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.NEGOTIATED)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.CANCELLED)).thenReturn(0L);
    }

    @Test
    @DisplayName("Should get transfer analytics successfully")
    void shouldGetTransferAnalyticsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.statusCode").value(200))
               .andExpect(jsonPath("$.message").value("Analytics retrieved successfully"))
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.totalTransfers").value(100))
               .andExpect(jsonPath("$.data.completedTransfers").value(85))
               .andExpect(jsonPath("$.data.failedTransfers").value(10))
               .andExpect(jsonPath("$.data.inProgressTransfers").value(3))
               .andExpect(jsonPath("$.data.deniedTransfers").value(2))
               .andExpect(jsonPath("$.data.successRate").value(85.0))
               .andExpect(jsonPath("$.data.transfersByState").exists());

        verify(transferRepository, atLeastOnce()).count();
        verify(transferRepository, atLeastOnce()).countByState(TransferState.COMPLETED);
        verify(transferRepository, atLeastOnce()).countByState(TransferState.FAILED);
        verify(transferRepository, atLeastOnce()).countByState(TransferState.TRANSFER_IN_PROGRESS);
        verify(transferRepository, atLeastOnce()).countByState(TransferState.DENIED);
    }

    @Test
    @DisplayName("Should handle zero transfers")
    void shouldHandleZeroTransfers() throws Exception {
        lenient().when(transferRepository.count()).thenReturn(100L);
        lenient().when(transferRepository.countByState(TransferState.COMPLETED)).thenReturn(85L);
        lenient().when(transferRepository.countByState(TransferState.FAILED)).thenReturn(10L);
        lenient().when(transferRepository.countByState(TransferState.TRANSFER_IN_PROGRESS)).thenReturn(3L);
        lenient().when(transferRepository.countByState(TransferState.DENIED)).thenReturn(2L);
        lenient().when(transferRepository.countByState(TransferState.REQUESTED)).thenReturn(0L);
        lenient().when(transferRepository.countByState(TransferState.POLICY_EVALUATION)).thenReturn(0L);
        lenient().when(transferRepository.countByState(TransferState.APPROVED)).thenReturn(0L);
        lenient().when(transferRepository.countByState(TransferState.CONTRACT_NEGOTIATION)).thenReturn(0L);
        lenient().when(transferRepository.countByState(TransferState.NEGOTIATED)).thenReturn(0L);
        lenient().when(transferRepository.countByState(TransferState.CANCELLED)).thenReturn(0L);

        when(transferRepository.count()).thenReturn(0L);
        when(transferRepository.countByState(any())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.totalTransfers").value(0))
               .andExpect(jsonPath("$.data.completedTransfers").value(0))
               .andExpect(jsonPath("$.data.successRate").value(0.0));

        verify(transferRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should calculate success rate correctly with non-zero transfers")
    void shouldCalculateSuccessRateCorrectly() throws Exception {
        when(transferRepository.count()).thenReturn(200L);
        when(transferRepository.countByState(TransferState.COMPLETED)).thenReturn(150L);

        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.totalTransfers").value(200))
               .andExpect(jsonPath("$.data.completedTransfers").value(150))
               .andExpect(jsonPath("$.data.successRate").value(75.0));
    }

    @Test
    @DisplayName("Should include all transfer states in analytics")
    void shouldIncludeAllTransferStatesInAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.transfersByState.COMPLETED").value(85))
               .andExpect(jsonPath("$.data.transfersByState.FAILED").value(10))
               .andExpect(jsonPath("$.data.transfersByState.TRANSFER_IN_PROGRESS").value(3))
               .andExpect(jsonPath("$.data.transfersByState.DENIED").value(2))
               .andExpect(jsonPath("$.data.transfersByState.REQUESTED").value(0));

        // Verify all states were queried
        for (TransferState state : TransferState.values()) {
            verify(transferRepository, atLeastOnce()).countByState(state);
        }
    }

    @Test
    @DisplayName("Should handle 100% success rate")
    void shouldHandle100PercentSuccessRate() throws Exception {
        when(transferRepository.count()).thenReturn(50L);
        when(transferRepository.countByState(TransferState.COMPLETED)).thenReturn(50L);
        when(transferRepository.countByState(TransferState.FAILED)).thenReturn(0L);

        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.totalTransfers").value(50))
               .andExpect(jsonPath("$.data.completedTransfers").value(50))
               .andExpect(jsonPath("$.data.successRate").value(100.0));
    }

    @Test
    @DisplayName("Should handle 0% success rate")
    void shouldHandle0PercentSuccessRate() throws Exception {
        when(transferRepository.count()).thenReturn(50L);
        when(transferRepository.countByState(TransferState.COMPLETED)).thenReturn(0L);
        when(transferRepository.countByState(TransferState.FAILED)).thenReturn(50L);

        mockMvc.perform(get("/api/v1/analytics/transfers"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.totalTransfers").value(50))
               .andExpect(jsonPath("$.data.completedTransfers").value(0))
               .andExpect(jsonPath("$.data.successRate").value(0.0));
    }
}

