package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UsagePolicyEvaluator
 */
class UsagePolicyEvaluatorTest {

    private final UsagePolicyEvaluator evaluator = new UsagePolicyEvaluator();

    @Test
    @DisplayName("Should allow approved usage purpose")
    void shouldAllowApprovedUsagePurpose() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .usagePurpose("QUALITY_ANALYSIS")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertTrue(result.isAllowed());
        assertTrue(result.getViolatedPolicies().isEmpty());
    }

    @Test
    @DisplayName("Should deny unapproved usage purpose")
    void shouldDenyUnapprovedUsagePurpose() {
        TransferRequest request = TransferRequest.builder()
            .consumerId("consumer1")
            .usagePurpose("MARKETING")
            .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertFalse(result.isAllowed());
        assertFalse(result.getViolatedPolicies().isEmpty());
    }
}

