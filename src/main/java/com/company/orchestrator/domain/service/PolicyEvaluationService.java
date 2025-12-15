package com.company.orchestrator.domain.service;

import com.company.orchestrator.config.CacheConfig;
import com.company.orchestrator.domain.enums.PolicyType;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.persistence.entity.PolicyEntity;
import com.company.orchestrator.infrastructure.persistence.repository.PolicyRepository;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final Optional<PolicyRepository> policyRepository;

    /**
     * Evaluates all active policies for a transfer request.
     * If a PolicyRepository is present, only policies that are marked active
     * in the database are evaluated; otherwise, all PolicyEvaluators are used
     * (preserving previous in-memory behavior for tests).
     */
    public PolicyEvaluationResult evaluateAll(TransferRequest request) {
        log.info("Evaluating policies for transfer request. Consumer: {}, Asset: {}",
            request.getConsumerId(), request.getAssetId());

        // Resolve which evaluators to run based on active DB policies when repository is available
        List<PolicyEvaluator> evaluatorsToRun = resolveEvaluatorsToRun();

        List<String> allViolated = new ArrayList<>();
        List<String> allSatisfied = new ArrayList<>();
        boolean allPassed = true;

        for (PolicyEvaluator evaluator : evaluatorsToRun) {
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
     * Evaluates specific policies by type. The passed-in policyTypes are
     * matched against available evaluators; when a PolicyRepository is present
     * only those that are also active in DB are considered.
     */
    public PolicyEvaluationResult evaluateSpecific(TransferRequest request, List<String> policyTypes) {
        log.info("Evaluating specific policies: {}", policyTypes);

        Map<String, PolicyEvaluator> evaluatorMap = policyEvaluators.stream()
            .collect(Collectors.toMap(PolicyEvaluator::getPolicyType, e -> e));

        // If repository is present, filter the requested policy types by active DB policies
        List<String> effectivePolicyTypes = policyRepository
            .map(repo -> {
                List<PolicyEntity> active = repo.findByActiveTrue();
                List<String> activeTypes = active.stream()
                    .map(pe -> pe.getType().name())
                    .distinct()
                    .toList();
                return policyTypes.stream()
                    .filter(activeTypes::contains)
                    .toList();
            })
            .orElse(policyTypes);

        List<String> allViolated = new ArrayList<>();
        List<String> allSatisfied = new ArrayList<>();
        boolean allPassed = true;

        for (String policyType : effectivePolicyTypes) {
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
     * Helper to resolve which evaluators should run for evaluateAll().
     * If a PolicyRepository is present, only evaluators whose type is present
     * as an active PolicyEntity.type are returned; otherwise returns
     * the full list of evaluators.
     */
    private List<PolicyEvaluator> resolveEvaluatorsToRun() {
        if (policyRepository.isEmpty()) {
            return policyEvaluators;
        }

        List<PolicyEntity> activePolicies = getActivePoliciesCached();
        if (activePolicies.isEmpty()) {
            log.warn("No active policies found in database; no policies will be evaluated");
            return List.of();
        }

        Map<String, PolicyEvaluator> evaluatorMap = policyEvaluators.stream()
            .collect(Collectors.toMap(PolicyEvaluator::getPolicyType, e -> e));

        return activePolicies.stream()
            .map(PolicyEntity::getType)
            .map(PolicyType::name)
            .distinct()
            .map(evaluatorMap::get)
            .filter(e -> {
                if (e == null) {
                    log.warn("Active policy has no matching evaluator configured");
                    return false;
                }
                return true;
            })
            .toList();
    }

    /**
     * Returns active policies from cache when repository is available.
     * The cache key is a constant because active policies are global, not per-request.
     */
    @Cacheable(cacheNames = CacheConfig.ACTIVE_POLICIES_CACHE, key = "'all'", unless = "#result == null")
    public List<PolicyEntity> getActivePoliciesCached() {
        return policyRepository
                .map(PolicyRepository::findByActiveTrue)
                .orElseGet(List::of);
    }

    /**
     * Lists all available policy types. When a PolicyRepository is present,
     * this is driven by active policies in the database; otherwise it falls
     * back to the types exposed by the configured evaluators.
     */
    public List<String> getAvailablePolicyTypes() {
        return policyRepository
            .map(repo -> getActivePoliciesCached().stream()
                .map(PolicyEntity::getType)
                .map(PolicyType::name)
                .distinct()
                .toList())
            .orElseGet(() -> policyEvaluators.stream()
                .map(PolicyEvaluator::getPolicyType)
                .collect(Collectors.toList()));
    }
}
