package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Evaluates geographic restriction policies
 * Example: "Data must not leave EU region"
 */
@Component
@Slf4j
public class GeographicPolicyEvaluator implements PolicyEvaluator {

    private static final Set<String> ALLOWED_REGIONS = Set.of("EU", "EEA", "GERMANY", "FRANCE", "ITALY");

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        String consumerRegion = request.getConsumerRegion();
        boolean allowed = consumerRegion != null &&
                         ALLOWED_REGIONS.stream()
                             .anyMatch(region -> region.equalsIgnoreCase(consumerRegion));

        List<String> violated = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        String policyName = "GEOGRAPHIC: EU/EEA Only";

        if (allowed) {
            satisfied.add(policyName);
            log.debug("Geographic policy satisfied. Consumer region: {}", consumerRegion);
        } else {
            violated.add(policyName);
            log.warn("Geographic policy violated. Consumer region: {}", consumerRegion);
        }

        return PolicyEvaluationResult.builder()
            .allowed(allowed)
            .violatedPolicies(violated)
            .satisfiedPolicies(satisfied)
            .reason(allowed ?
                String.format("Access granted for region: %s", consumerRegion) :
                String.format("Access denied. Region '%s' is not in allowed list: %s",
                    consumerRegion, ALLOWED_REGIONS))
            .build();
    }

    @Override
    public String getPolicyType() {
        return "GEOGRAPHIC";
    }
}

