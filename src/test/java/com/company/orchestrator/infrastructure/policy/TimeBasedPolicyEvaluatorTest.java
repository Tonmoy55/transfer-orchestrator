package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimeBasedPolicyEvaluator
 */
class TimeBasedPolicyEvaluatorTest {

    private final TimeBasedPolicyEvaluator evaluator = new TimeBasedPolicyEvaluator();

    @Test
    @DisplayName("Should return policy type")
    void shouldReturnPolicyType() {
        assertEquals("TIME_BASED", evaluator.getPolicyType());
    }

    @Test
    @DisplayName("Should evaluate time-based policy")
    void shouldEvaluateTimeBasedPolicy() {
        TransferRequest request = TransferRequest.builder()
                                                 .consumerId("consumer1")
                                                 .providerId("provider1")
                                                 .assetId("asset1")
                                                 .build();

        PolicyEvaluationResult result = evaluator.evaluate(request);

        assertNotNull(result);
        // Result depends on current time
        assertNotNull(result.getReason());
    }
}
