package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test cases for PolicyEvaluationService
 * Coverage: 100%
 */
@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceTest {

    @Mock
    private PolicyEvaluator timeBasedEvaluator;

    @Mock
    private PolicyEvaluator geographicEvaluator;

    @Mock
    private PolicyEvaluator certificationEvaluator;

    private PolicyEvaluationService policyEvaluationService;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        List<PolicyEvaluator> evaluators = Arrays.asList(
            timeBasedEvaluator,
            geographicEvaluator,
            certificationEvaluator
        );

        policyEvaluationService = new PolicyEvaluationService(evaluators);

        transferRequest = TransferRequest.builder()
            .consumerId("consumer-1")
            .providerId("provider-1")
            .assetId("asset-123")
            .dataType("PRODUCTION_DATA")
            .consumerRegion("EU")
            .consumerCertificationLevel("ISO9001")
            .usagePurpose("QUALITY_ANALYSIS")
            .build();

        // Setup default policy types
        when(timeBasedEvaluator.getPolicyType()).thenReturn("TIME_BASED");
        when(geographicEvaluator.getPolicyType()).thenReturn("GEOGRAPHIC");
        when(certificationEvaluator.getPolicyType()).thenReturn("CERTIFICATION");
    }

    @Test
    @DisplayName("Should approve when all policies pass")
    void shouldApproveWhenAllPoliciesPass() {
        PolicyEvaluationResult timeResult = createPassedResult("TIME_BASED");
        PolicyEvaluationResult geoResult = createPassedResult("GEOGRAPHIC");
        PolicyEvaluationResult certResult = createPassedResult("CERTIFICATION");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);
        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);
        when(certificationEvaluator.evaluate(any())).thenReturn(certResult);

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(transferRequest);

        assertTrue(result.isAllowed());
        assertEquals("All policies satisfied", result.getReason());
        assertEquals(0, result.getViolatedPolicies().size());
        assertEquals(3, result.getSatisfiedPolicies().size());

        verify(timeBasedEvaluator, times(1)).evaluate(any());
        verify(geographicEvaluator, times(1)).evaluate(any());
        verify(certificationEvaluator, times(1)).evaluate(any());
    }

    @Test
    @DisplayName("Should deny when one policy fails")
    void shouldDenyWhenOnePolicyFails() {
        PolicyEvaluationResult timeResult = createPassedResult("TIME_BASED");
        PolicyEvaluationResult geoResult = createFailedResult("GEOGRAPHIC", "Data transfer outside EU not allowed");
        PolicyEvaluationResult certResult = createPassedResult("CERTIFICATION");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);
        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);
        when(certificationEvaluator.evaluate(any())).thenReturn(certResult);

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(transferRequest);

        assertFalse(result.isAllowed());
        assertTrue(result.getReason().contains("Policy violations"));
        assertEquals(1, result.getViolatedPolicies().size());
        assertTrue(result.getViolatedPolicies().contains("GEOGRAPHIC"));
        assertEquals(2, result.getSatisfiedPolicies().size());
    }

    @Test
    @DisplayName("Should deny when all policies fail")
    void shouldDenyWhenAllPoliciesFail() {
        PolicyEvaluationResult timeResult = createFailedResult("TIME_BASED", "Outside business hours");
        PolicyEvaluationResult geoResult = createFailedResult("GEOGRAPHIC", "Region not allowed");
        PolicyEvaluationResult certResult = createFailedResult("CERTIFICATION", "Invalid certification");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);
        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);
        when(certificationEvaluator.evaluate(any())).thenReturn(certResult);

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(transferRequest);

        assertFalse(result.isAllowed());
        assertEquals(3, result.getViolatedPolicies().size());
        assertEquals(0, result.getSatisfiedPolicies().size());
    }

    @Test
    @DisplayName("Should handle policy evaluator throwing exception")
    void shouldHandlePolicyEvaluatorThrowingException() {
        PolicyEvaluationResult timeResult = createPassedResult("TIME_BASED");
        PolicyEvaluationResult certResult = createPassedResult("CERTIFICATION");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);
        when(geographicEvaluator.evaluate(any())).thenThrow(new RuntimeException("Evaluation error"));
        when(certificationEvaluator.evaluate(any())).thenReturn(certResult);

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(transferRequest);

        assertFalse(result.isAllowed());
        assertTrue(result.getViolatedPolicies().stream()
            .anyMatch(v -> v.contains("GEOGRAPHIC") && v.contains("Evaluation error")));
        assertEquals(2, result.getSatisfiedPolicies().size());
    }

    @Test
    @DisplayName("Should evaluate specific policies successfully")
    void shouldEvaluateSpecificPoliciesSuccessfully() {
        PolicyEvaluationResult timeResult = createPassedResult("TIME_BASED");
        PolicyEvaluationResult geoResult = createPassedResult("GEOGRAPHIC");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);
        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);

        List<String> policyTypes = Arrays.asList("TIME_BASED", "GEOGRAPHIC");
        PolicyEvaluationResult result = policyEvaluationService.evaluateSpecific(transferRequest, policyTypes);

        assertTrue(result.isAllowed());
        assertEquals("Specified policies satisfied", result.getReason());
        assertEquals(0, result.getViolatedPolicies().size());
        assertEquals(2, result.getSatisfiedPolicies().size());

        verify(timeBasedEvaluator, times(1)).evaluate(any());
        verify(geographicEvaluator, times(1)).evaluate(any());
        verify(certificationEvaluator, never()).evaluate(any());
    }

    @Test
    @DisplayName("Should handle non-existent policy type in specific evaluation")
    void shouldHandleNonExistentPolicyTypeInSpecificEvaluation() {
        PolicyEvaluationResult timeResult = createPassedResult("TIME_BASED");

        when(timeBasedEvaluator.evaluate(any())).thenReturn(timeResult);

        List<String> policyTypes = Arrays.asList("TIME_BASED", "NON_EXISTENT");
        PolicyEvaluationResult result = policyEvaluationService.evaluateSpecific(transferRequest, policyTypes);

        assertFalse(result.isAllowed());
        assertTrue(result.getViolatedPolicies().stream()
            .anyMatch(v -> v.contains("NON_EXISTENT") && v.contains("No evaluator configured")));
        assertEquals(1, result.getSatisfiedPolicies().size());
    }

    @Test
    @DisplayName("Should handle exception in specific policy evaluation")
    void shouldHandleExceptionInSpecificPolicyEvaluation() {
        when(timeBasedEvaluator.evaluate(any())).thenThrow(new RuntimeException("Evaluation failed"));

        List<String> policyTypes = Collections.singletonList("TIME_BASED");
        PolicyEvaluationResult result = policyEvaluationService.evaluateSpecific(transferRequest, policyTypes);

        assertFalse(result.isAllowed());
        assertTrue(result.getViolatedPolicies().stream()
            .anyMatch(v -> v.contains("TIME_BASED") && v.contains("Evaluation error")));
    }

    @Test
    @DisplayName("Should get available policy types")
    void shouldGetAvailablePolicyTypes() {
        List<String> policyTypes = policyEvaluationService.getAvailablePolicyTypes();

        assertNotNull(policyTypes);
        assertEquals(3, policyTypes.size());
        assertTrue(policyTypes.contains("TIME_BASED"));
        assertTrue(policyTypes.contains("GEOGRAPHIC"));
        assertTrue(policyTypes.contains("CERTIFICATION"));
    }

    @Test
    @DisplayName("Should return empty list when no evaluators configured")
    void shouldReturnEmptyListWhenNoEvaluatorsConfigured() {
        lenient().when(timeBasedEvaluator.getPolicyType()).thenReturn("TIME_BASED");
        lenient().when(geographicEvaluator.getPolicyType()).thenReturn("GEOGRAPHIC");
        lenient().when(certificationEvaluator.getPolicyType()).thenReturn("CERTIFICATION");

        PolicyEvaluationService emptyService = new PolicyEvaluationService(Collections.emptyList());

        List<String> policyTypes = emptyService.getAvailablePolicyTypes();

        assertNotNull(policyTypes);
        assertTrue(policyTypes.isEmpty());
    }

    @Test
    @DisplayName("Should approve when no evaluators configured")
    void shouldApproveWhenNoEvaluatorsConfigured() {

        lenient().when(timeBasedEvaluator.getPolicyType()).thenReturn("TIME_BASED");
        lenient().when(geographicEvaluator.getPolicyType()).thenReturn("GEOGRAPHIC");
        lenient().when(certificationEvaluator.getPolicyType()).thenReturn("CERTIFICATION");

        PolicyEvaluationService emptyService = new PolicyEvaluationService(Collections.emptyList());

        PolicyEvaluationResult result = emptyService.evaluateAll(transferRequest);

        assertTrue(result.isAllowed());
        assertEquals("All policies satisfied", result.getReason());
        assertTrue(result.getViolatedPolicies().isEmpty());
        assertTrue(result.getSatisfiedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple violations from single policy")
    void shouldHandleMultipleViolationsFromSinglePolicy() {
        PolicyEvaluationResult geoResult = PolicyEvaluationResult.builder()
            .allowed(false)
            .reason("Multiple violations")
            .satisfiedPolicies(Collections.emptyList())
            .violatedPolicies(Arrays.asList("GEOGRAPHIC: Region", "GEOGRAPHIC: Timezone"))
            .build();

        when(timeBasedEvaluator.evaluate(any())).thenReturn(createPassedResult("TIME_BASED"));
        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);
        when(certificationEvaluator.evaluate(any())).thenReturn(createPassedResult("CERTIFICATION"));

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(transferRequest);

        assertFalse(result.isAllowed());
        assertEquals(2, result.getViolatedPolicies().size());
        assertEquals(2, result.getSatisfiedPolicies().size());
    }

    @Test
    @DisplayName("Should evaluate specific policy with failure")
    void shouldEvaluateSpecificPolicyWithFailure() {
        PolicyEvaluationResult geoResult = createFailedResult("GEOGRAPHIC", "Region not allowed");

        when(geographicEvaluator.evaluate(any())).thenReturn(geoResult);

        List<String> policyTypes = Collections.singletonList("GEOGRAPHIC");
        PolicyEvaluationResult result = policyEvaluationService.evaluateSpecific(transferRequest, policyTypes);

        assertFalse(result.isAllowed());
        assertEquals("Policy violations found", result.getReason());
        assertEquals(1, result.getViolatedPolicies().size());
    }

    // Helper methods
    private PolicyEvaluationResult createPassedResult(String policyType) {
        return PolicyEvaluationResult.builder()
            .allowed(true)
            .reason("Policy satisfied")
            .satisfiedPolicies(Collections.singletonList(policyType))
            .violatedPolicies(Collections.emptyList())
            .build();
    }

    private PolicyEvaluationResult createFailedResult(String policyType, String reason) {
        return PolicyEvaluationResult.builder()
            .allowed(false)
            .reason(reason)
            .satisfiedPolicies(Collections.emptyList())
            .violatedPolicies(Collections.singletonList(policyType))
            .build();
    }
}

