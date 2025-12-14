package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for PolicyEvaluationService
 */
@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceIntegrationTest {

    @Mock
    private PolicyEvaluator timeBasedEvaluator;

    @Mock
    private PolicyEvaluator geographicEvaluator;

    @Mock
    private PolicyEvaluator certificationEvaluator;

    @Mock
    private PolicyEvaluator usageEvaluator;

    @Mock
    private PolicyEvaluator rateLimitEvaluator;

    private PolicyEvaluationService policyEvaluationService;
    private TransferRequest request;

    @BeforeEach
    void setUp() {

        policyEvaluationService = new PolicyEvaluationService(
                List.of(
                        timeBasedEvaluator,
                        geographicEvaluator,
                        certificationEvaluator,
                        usageEvaluator,
                        rateLimitEvaluator
                )
        );

        request = TransferRequest.builder()
                                 .consumerId("consumer-1")
                                 .providerId("provider-1")
                                 .assetId("asset-123")
                                 .consumerRegion("EU")
                                 .consumerCertificationLevel("ISO9001")
                                 .usagePurpose("QUALITY_ANALYSIS")
                                 .build();

        when(timeBasedEvaluator.getPolicyType()).thenReturn("TIME_BASED");
        when(geographicEvaluator.getPolicyType()).thenReturn("GEOGRAPHIC");
        when(certificationEvaluator.getPolicyType()).thenReturn("CERTIFICATION");
        when(usageEvaluator.getPolicyType()).thenReturn("USAGE");
        when(rateLimitEvaluator.getPolicyType()).thenReturn("RATE_LIMIT");
    }

    @Test
    @DisplayName("Should evaluate all policies successfully")
    void shouldEvaluateAllPoliciesSuccessfully() {

        when(timeBasedEvaluator.evaluate(any())).thenReturn(pass("TIME_BASED"));
        when(geographicEvaluator.evaluate(any())).thenReturn(pass("GEOGRAPHIC"));
        when(certificationEvaluator.evaluate(any())).thenReturn(pass("CERTIFICATION"));
        when(usageEvaluator.evaluate(any())).thenReturn(pass("USAGE"));
        when(rateLimitEvaluator.evaluate(any())).thenReturn(pass("RATE_LIMIT"));

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(request);

        assertTrue(result.isAllowed());
        assertEquals("All policies satisfied", result.getReason());
        assertEquals(5, result.getSatisfiedPolicies().size());
        assertTrue(result.getViolatedPolicies().isEmpty());
    }


    @Test
    @DisplayName("Should fail when any policy is violated")
    void shouldFailWhenAnyPolicyIsViolated() {

        when(timeBasedEvaluator.evaluate(any())).thenReturn(pass("TIME_BASED"));
        when(geographicEvaluator.evaluate(any()))
                .thenReturn(fail("GEOGRAPHIC", "Region not allowed"));
        when(certificationEvaluator.evaluate(any())).thenReturn(pass("CERTIFICATION"));
        when(usageEvaluator.evaluate(any())).thenReturn(pass("USAGE"));
        when(rateLimitEvaluator.evaluate(any())).thenReturn(pass("RATE_LIMIT"));

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(request);

        assertFalse(result.isAllowed());
        assertFalse(result.getViolatedPolicies().isEmpty());
        assertTrue(result.getViolatedPolicies().contains("GEOGRAPHIC"));
    }


    @Test
    @DisplayName("Should list available policy types")
    void shouldListAvailablePolicyTypes() {

        List<String> policyTypes = policyEvaluationService.getAvailablePolicyTypes();

        assertNotNull(policyTypes);
        assertEquals(5, policyTypes.size());
        assertTrue(policyTypes.contains("TIME_BASED"));
        assertTrue(policyTypes.contains("GEOGRAPHIC"));
        assertTrue(policyTypes.contains("CERTIFICATION"));
        assertTrue(policyTypes.contains("USAGE"));
        assertTrue(policyTypes.contains("RATE_LIMIT"));
    }


    private PolicyEvaluationResult pass(String policyType) {
        return PolicyEvaluationResult.builder()
                                     .allowed(true)
                                     .satisfiedPolicies(List.of(policyType))
                                     .violatedPolicies(List.of())
                                     .reason("Policy satisfied")
                                     .build();
    }

    private PolicyEvaluationResult fail(String policyType, String reason) {
        return PolicyEvaluationResult.builder()
                                     .allowed(false)
                                     .satisfiedPolicies(List.of())
                                     .violatedPolicies(List.of(policyType))
                                     .reason(reason)
                                     .build();
    }
}

