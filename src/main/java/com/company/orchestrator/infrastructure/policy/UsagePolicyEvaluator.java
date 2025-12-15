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
 * Evaluates usage purpose policies
 * Example: "Data can only be used for quality analysis"
 */
@Component
@Slf4j
public class UsagePolicyEvaluator implements PolicyEvaluator {

    private static final Set<String> ALLOWED_PURPOSES = Set.of(
        "QUALITY_ANALYSIS",
        "SUPPLY_CHAIN_OPTIMIZATION",
        "COMPLIANCE_REPORTING"
    );

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        String usagePurpose = request.getUsagePurpose();
        boolean allowed = usagePurpose != null &&
                         ALLOWED_PURPOSES.stream()
                             .anyMatch(purpose -> purpose.equalsIgnoreCase(usagePurpose));

        List<String> violated = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        String policyName = "USAGE: Approved Purposes Only";

        if (allowed) {
            satisfied.add(policyName);
            log.debug("Usage policy satisfied. Purpose: {}", usagePurpose);
        } else {
            violated.add(policyName);
            log.warn("Usage policy violated. Purpose: {}", usagePurpose);
        }

        return PolicyEvaluationResult.builder()
            .allowed(allowed)
            .violatedPolicies(violated)
            .satisfiedPolicies(satisfied)
            .reason(allowed ?
                String.format("Usage purpose '%s' is allowed", usagePurpose) :
                String.format("Usage purpose '%s' is not in allowed list: %s",
                    usagePurpose, ALLOWED_PURPOSES))
            .build();
    }

    @Override
    public String getPolicyType() {
        return "USAGE";
    }
}

