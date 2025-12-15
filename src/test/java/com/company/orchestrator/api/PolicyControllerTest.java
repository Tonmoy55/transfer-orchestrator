package com.company.orchestrator.api;

import com.company.orchestrator.api.dto.PolicyDto;
import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.enums.PolicyType;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.service.PolicyEvaluationService;
import com.company.orchestrator.domain.service.PolicyQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive test cases for PolicyController
 * Coverage: 100%
 */
@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private PolicyEvaluationService policyEvaluationService;

    @Mock
    private PolicyQueryService policyQueryService;

    @InjectMocks
    private PolicyController policyController;

    private TransferRequestDto validRequest;
    private PolicyEvaluationResult passedResult;
    private PolicyEvaluationResult failedResult;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        // Build MockMvc with the real controller and a mocked service
        mockMvc = MockMvcBuilders
                .standaloneSetup(policyController)
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

        passedResult = PolicyEvaluationResult.builder()
                                             .allowed(true)
                                             .reason("All policies satisfied")
                                             .satisfiedPolicies(Arrays.asList("TIME_BASED", "GEOGRAPHIC", "CERTIFICATION"))
                                             .violatedPolicies(Collections.emptyList())
                                             .build();

        failedResult = PolicyEvaluationResult.builder()
                                             .allowed(false)
                                             .reason("Geographic policy violation: Data transfer outside EU region not allowed")
                                             .satisfiedPolicies(Arrays.asList("TIME_BASED", "CERTIFICATION"))
                                             .violatedPolicies(Collections.singletonList("GEOGRAPHIC"))
                                             .build();
    }

    @Test
    @DisplayName("Should evaluate policy successfully when policies pass")
    void shouldEvaluatePolicySuccessfullyWhenPoliciesPass() throws Exception {
        when(policyEvaluationService.evaluateAll(any()))
                .thenReturn(passedResult);

        mockMvc.perform(post("/api/v1/policies/evaluate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(validRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.statusCode").value(200))
               .andExpect(jsonPath("$.message").value("Policy evaluation passed"))
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.allowed").value(true))
               .andExpect(jsonPath("$.data.reason").value("All policies satisfied"))
               // PolicyEvaluationResult exposes satisfiedPolicies & violatedPolicies, not evaluatedPolicies
               .andExpect(jsonPath("$.data.satisfiedPolicies", hasSize(3)))
               .andExpect(jsonPath("$.data.violatedPolicies", hasSize(0)));

        verify(policyEvaluationService, times(1)).evaluateAll(any());
    }

    @Test
    @DisplayName("Should evaluate policy and return failure when policies fail")
    void shouldEvaluatePolicyAndReturnFailureWhenPoliciesFail() throws Exception {
        when(policyEvaluationService.evaluateAll(any()))
                .thenReturn(failedResult);

        mockMvc.perform(post("/api/v1/policies/evaluate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(validRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.statusCode").value(200))
               .andExpect(jsonPath("$.message").value("Policy evaluation failed: Geographic policy violation: Data transfer outside EU region not allowed"))
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.allowed").value(false))
               .andExpect(jsonPath("$.data.violatedPolicies", hasSize(1)))
               .andExpect(jsonPath("$.data.violatedPolicies[0]").value("GEOGRAPHIC"));

        verify(policyEvaluationService, times(1)).evaluateAll(any());
    }

    @Test
    @DisplayName("Should return validation error for invalid request")
    void shouldReturnValidationErrorForInvalidRequest() throws Exception {
        TransferRequestDto invalidRequest = TransferRequestDto.builder()
                                                              .consumerId("consumer-1")
                                                              .build();

        mockMvc.perform(post("/api/v1/policies/evaluate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());

        verify(policyEvaluationService, never()).evaluateAll(any());
    }

    @Test
    @DisplayName("Should get available policy types successfully")
    void shouldGetAvailablePolicyTypesSuccessfully() throws Exception {
        List<String> policyTypes = Arrays.asList(
                "TIME_BASED",
                "GEOGRAPHIC",
                "CERTIFICATION",
                "RATE_LIMIT",
                "USAGE"
        );

        when(policyEvaluationService.getAvailablePolicyTypes())
                .thenReturn(policyTypes);

        mockMvc.perform(get("/api/v1/policies/types"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.statusCode").value(200))
               .andExpect(jsonPath("$.message").value("Policy types retrieved successfully"))
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data", hasSize(5)))
               .andExpect(jsonPath("$.data[0]").value("TIME_BASED"))
               .andExpect(jsonPath("$.data[1]").value("GEOGRAPHIC"))
               .andExpect(jsonPath("$.data[2]").value("CERTIFICATION"))
               .andExpect(jsonPath("$.data[3]").value("RATE_LIMIT"))
               .andExpect(jsonPath("$.data[4]").value("USAGE"));

        verify(policyEvaluationService, times(1)).getAvailablePolicyTypes();
    }

    @Test
    @DisplayName("Should handle empty policy types list")
    void shouldHandleEmptyPolicyTypesList() throws Exception {
        when(policyEvaluationService.getAvailablePolicyTypes())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/policies/types"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data", hasSize(0)));

        verify(policyEvaluationService, times(1)).getAvailablePolicyTypes();
    }

    @Test
    @DisplayName("Should evaluate policy with all required fields")
    void shouldEvaluatePolicyWithAllRequiredFields() throws Exception {
        TransferRequestDto fullRequest = TransferRequestDto.builder()
                                                           .consumerId("consumer-1")
                                                           .providerId("provider-1")
                                                           .assetId("asset-123")
                                                           .dataType("PRODUCTION_DATA")
                                                           .consumerRegion("EU")
                                                           .consumerCertificationLevel("ISO9001")
                                                           .usagePurpose("QUALITY_ANALYSIS")
                                                           .policyIds(Arrays.asList("policy-1", "policy-2"))
                                                           .build();

        when(policyEvaluationService.evaluateAll(any()))
                .thenReturn(passedResult);

        mockMvc.perform(post("/api/v1/policies/evaluate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(fullRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.allowed").value(true));

        verify(policyEvaluationService, times(1)).evaluateAll(any());
    }

    @Test
    @DisplayName("Should handle policy evaluation with empty satisfied policies")
    void shouldHandlePolicyEvaluationWithEmptySatisfiedPolicies() throws Exception {
        PolicyEvaluationResult allFailedResult = PolicyEvaluationResult.builder()
                                                                       .allowed(false)
                                                                       .reason("All policies failed")
                                                                       .satisfiedPolicies(Collections.emptyList())
                                                                       .violatedPolicies(Arrays.asList("TIME_BASED", "GEOGRAPHIC"))
                                                                       .build();

        when(policyEvaluationService.evaluateAll(any()))
                .thenReturn(allFailedResult);

        mockMvc.perform(post("/api/v1/policies/evaluate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(validRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.allowed").value(false))
               .andExpect(jsonPath("$.data.satisfiedPolicies", hasSize(0)))
               .andExpect(jsonPath("$.data.violatedPolicies", hasSize(2)));

        verify(policyEvaluationService, times(1)).evaluateAll(any());
    }

    @Test
    @DisplayName("Should list all policies with full details")
    void shouldListAllPoliciesWithFullDetails() throws Exception {
        PolicyDto policy = PolicyDto.builder()
                .id("1")
                .name("Time based EU only")
                .type(PolicyType.TIME_BASED)
                .description("Allow only in business hours")
                .configuration("{\"from\":\"08:00\",\"to\":\"18:00\"}")
                .active(true)
                .build();

        when(policyQueryService.getAllPolicies()).thenReturn(List.of(policy));

        mockMvc.perform(get("/api/v1/policies"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.statusCode").value(200))
               .andExpect(jsonPath("$.message").value("Policies retrieved successfully"))
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data", hasSize(1)))
               .andExpect(jsonPath("$.data[0].id").value("1"))
               .andExpect(jsonPath("$.data[0].name").value("Time based EU only"))
               .andExpect(jsonPath("$.data[0].type").value("TIME_BASED"))
               .andExpect(jsonPath("$.data[0].description").value("Allow only in business hours"))
               .andExpect(jsonPath("$.data[0].configuration").value("{\"from\":\"08:00\",\"to\":\"18:00\"}"))
               .andExpect(jsonPath("$.data[0].active").value(true));

        verify(policyQueryService, times(1)).getAllPolicies();
    }
}
