package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeographicPolicyEvaluator
 */
class GeographicPolicyEvaluatorTest {

    private final GeographicPolicyEvaluator evaluator = new GeographicPolicyEvaluator();

    @Test
    @DisplayName("Should allow EU region")
    void shouldAllowEURegion() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerRegion("EU")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertTrue(result.isAllowed());
        assertTrue(result.getViolatedPolicies().isEmpty());
        assertFalse(result.getSatisfiedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should deny non-EU region")
    void shouldDenyNonEURegion() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerRegion("US")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertFalse(result.isAllowed());
        assertFalse(result.getViolatedPolicies().isEmpty());
        assertTrue(result.getSatisfiedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should deny null region")
    void shouldDenyNullRegion() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerRegion(null)
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertFalse(result.isAllowed());
    }
}

