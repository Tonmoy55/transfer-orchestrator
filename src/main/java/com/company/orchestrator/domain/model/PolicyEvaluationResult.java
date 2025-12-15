package com.company.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of policy evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResult {
    private boolean allowed;
    private List<String> violatedPolicies;
    private List<String> satisfiedPolicies;
    private String reason;
}

