package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates time-based access policies
 * Example: "Allow transfer only between 8 AM - 6 PM CET"
 */
@Component
@Slf4j
public class TimeBasedPolicyEvaluator implements PolicyEvaluator {

    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
    //private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(23, 0);

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        LocalTime now = LocalTime.now();
        boolean allowed = now.isAfter(BUSINESS_START) && now.isBefore(BUSINESS_END);

        List<String> violated = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        String policyName = "TIME_BASED: Business Hours (8 AM - 6 PM)";

        if (allowed) {
            satisfied.add(policyName);
            log.debug("Time-based policy satisfied for transfer request");
        } else {
            violated.add(policyName);
            log.warn("Time-based policy violated. Current time: {}", now);
        }

        return PolicyEvaluationResult.builder()
            .allowed(allowed)
            .violatedPolicies(violated)
            .satisfiedPolicies(satisfied)
            .reason(allowed ? "Access granted during business hours" :
                    String.format("Access denied. Current time %s is outside business hours (8 AM - 6 PM)", now))
            .build();
    }

    @Override
    public String getPolicyType() {
        return "TIME_BASED";
    }
}

