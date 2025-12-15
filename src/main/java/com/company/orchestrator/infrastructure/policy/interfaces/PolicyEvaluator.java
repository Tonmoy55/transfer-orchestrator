package com.company.orchestrator.infrastructure.policy.interfaces;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;

/**
 * Base interfaces for policy evaluators
 */
public interface PolicyEvaluator {

    /**
     * Evaluates if the transfer request satisfies the policy
     */
    PolicyEvaluationResult evaluate(TransferRequest request);

    /**
     * Returns the policy type this evaluator handles
     */
    String getPolicyType();
}
