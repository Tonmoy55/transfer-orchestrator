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
 * Evaluates certification requirement policies
 * Example: "Consumer must have ISO 9001 certification"
 */
@Component
@Slf4j
public class CertificationPolicyEvaluator implements PolicyEvaluator {

    private static final Set<String> REQUIRED_CERTIFICATIONS = Set.of("ISO9001", "ISO27001", "TISAX");

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        String certLevel = request.getConsumerCertificationLevel();
        boolean allowed = certLevel != null &&
                         REQUIRED_CERTIFICATIONS.stream()
                             .anyMatch(cert -> cert.equalsIgnoreCase(certLevel));

        List<String> violated = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        String policyName = "CERTIFICATION: Required Quality Standards";

        if (allowed) {
            satisfied.add(policyName);
            log.debug("Certification policy satisfied. Consumer certification: {}", certLevel);
        } else {
            violated.add(policyName);
            log.warn("Certification policy violated. Consumer certification: {}", certLevel);
        }

        return PolicyEvaluationResult.builder()
            .allowed(allowed)
            .violatedPolicies(violated)
            .satisfiedPolicies(satisfied)
            .reason(allowed ?
                String.format("Certification '%s' is valid", certLevel) :
                String.format("Certification '%s' is not in required list: %s",
                    certLevel, REQUIRED_CERTIFICATIONS))
            .build();
    }

    @Override
    public String getPolicyType() {
        return "CERTIFICATION";
    }
}

