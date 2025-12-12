package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CertificationPolicyEvaluator
 */
class CertificationPolicyEvaluatorTest {

    private final CertificationPolicyEvaluator evaluator = new CertificationPolicyEvaluator();

    @Test
    @DisplayName("Should allow valid certification")
    void shouldAllowValidCertification() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerCertificationLevel("ISO9001")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertTrue(result.isAllowed());
        assertTrue(result.getViolatedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should deny invalid certification")
    void shouldDenyInvalidCertification() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerCertificationLevel("UNKNOWN")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertFalse(result.isAllowed());
        assertFalse(result.getViolatedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should be case insensitive")
    void shouldBeCaseInsensitive() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .consumerCertificationLevel("iso9001")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertTrue(result.isAllowed());
    }
}

