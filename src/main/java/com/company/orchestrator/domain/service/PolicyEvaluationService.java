package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for evaluating transfer policies
 * Critical Component: Supports composable policy evaluation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final List<PolicyEvaluator> policyEvaluators;

    /**
     * Evaluates all policies for a transfer request
     * Policies are composed with AND logic - all must pass
     */
    public PolicyEvaluationResult evaluateAll(TransferRequest request) {
        log.info("Evaluating policies for transfer request. Consumer: {}, Asset: {}",
            request.getConsumerId(), request.getAssetId());

        List<String> allViolated = new ArrayList<>();
        List<String> allSatisfied = new ArrayList<>();
        boolean allPassed = true;

        // Evaluate each policy
        for (PolicyEvaluator evaluator : policyEvaluators) {
            try {
                PolicyEvaluationResult result = evaluator.evaluate(request);

                if (!result.isAllowed()) {
                    allPassed = false;
                }

                allViolated.addAll(result.getViolatedPolicies());
                allSatisfied.addAll(result.getSatisfiedPolicies());

                log.debug("Policy {} evaluation: {}", evaluator.getPolicyType(),
                    result.isAllowed() ? "PASSED" : "FAILED");

            } catch (Exception e) {
                log.error("Error evaluating policy {}: {}", evaluator.getPolicyType(), e.getMessage(), e);
                allPassed = false;
                allViolated.add(evaluator.getPolicyType() + ": Evaluation error");
            }
        }

        String reason = allPassed ?
            "All policies satisfied" :
            String.format("Policy violations: %s", String.join(", ", allViolated));

        log.info("Policy evaluation complete. Result: {}. Violations: {}",
            allPassed ? "APPROVED" : "DENIED", allViolated.size());

        return PolicyEvaluationResult.builder()
            .allowed(allPassed)
            .violatedPolicies(allViolated)
            .satisfiedPolicies(allSatisfied)
            .reason(reason)
            .build();
    }

    /**
     * Evaluates specific policies by type
     */
    public PolicyEvaluationResult evaluateSpecific(TransferRequest request, List<String> policyTypes) {
        log.info("Evaluating specific policies: {}", policyTypes);

        Map<String, PolicyEvaluator> evaluatorMap = policyEvaluators.stream()
            .collect(Collectors.toMap(PolicyEvaluator::getPolicyType, e -> e));

        List<String> allViolated = new ArrayList<>();
        List<String> allSatisfied = new ArrayList<>();
        boolean allPassed = true;

        for (String policyType : policyTypes) {
            PolicyEvaluator evaluator = evaluatorMap.get(policyType);

            if (evaluator == null) {
                log.warn("No evaluator found for policy type: {}", policyType);
                allViolated.add(policyType + ": No evaluator configured");
                allPassed = false;
                continue;
            }

            try {
                PolicyEvaluationResult result = evaluator.evaluate(request);

                if (!result.isAllowed()) {
                    allPassed = false;
                }

                allViolated.addAll(result.getViolatedPolicies());
                allSatisfied.addAll(result.getSatisfiedPolicies());

            } catch (Exception e) {
                log.error("Error evaluating policy {}: {}", policyType, e.getMessage(), e);
                allPassed = false;
                allViolated.add(policyType + ": Evaluation error");
            }
        }

        return PolicyEvaluationResult.builder()
            .allowed(allPassed)
            .violatedPolicies(allViolated)
            .satisfiedPolicies(allSatisfied)
            .reason(allPassed ? "Specified policies satisfied" : "Policy violations found")
            .build();
    }

    /**
     * Lists all available policy types
     */
    public List<String> getAvailablePolicyTypes() {
        return policyEvaluators.stream()
            .map(PolicyEvaluator::getPolicyType)
            .collect(Collectors.toList());
    }
}

