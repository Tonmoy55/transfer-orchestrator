package com.company.orchestrator.infrastructure.policy;

import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.infrastructure.persistence.entity.RateLimitTrackingEntity;
import com.company.orchestrator.infrastructure.persistence.repository.RateLimitTrackingRepository;
import com.company.orchestrator.infrastructure.policy.interfaces.PolicyEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates rate limit policies
 * Example: "Maximum 100 requests per hour per consumer"
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitPolicyEvaluator implements PolicyEvaluator {

    private static final int MAX_REQUESTS_PER_HOUR = 100;
    private final RateLimitTrackingRepository rateLimitRepository;

    @Override
    @Transactional
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // Get current request count for the last hour
        Integer requestCount = rateLimitRepository
            .sumRequestCountByConsumerIdAndWindowStartAfter(request.getConsumerId(), oneHourAgo);

        int currentCount = requestCount != null ? requestCount : 0;
        boolean allowed = currentCount < MAX_REQUESTS_PER_HOUR;

        List<String> violated = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        String policyName = String.format("RATE_LIMIT: Max %d requests/hour", MAX_REQUESTS_PER_HOUR);

        if (allowed) {
            satisfied.add(policyName);
            log.debug("Rate limit policy satisfied. Current count: {}/{}", currentCount, MAX_REQUESTS_PER_HOUR);

            // Track this request
            trackRequest(request.getConsumerId(), now);
        } else {
            violated.add(policyName);
            log.warn("Rate limit exceeded for consumer: {}. Count: {}/{}",
                request.getConsumerId(), currentCount, MAX_REQUESTS_PER_HOUR);
        }

        return PolicyEvaluationResult.builder()
            .allowed(allowed)
            .violatedPolicies(violated)
            .satisfiedPolicies(satisfied)
            .reason(allowed ?
                String.format("Rate limit OK (%d/%d requests)", currentCount + 1, MAX_REQUESTS_PER_HOUR) :
                String.format("Rate limit exceeded (%d/%d requests)", currentCount, MAX_REQUESTS_PER_HOUR))
            .build();
    }

    private void trackRequest(String consumerId, LocalDateTime timestamp) {
        // Round to hour window
        LocalDateTime windowStart = timestamp.withMinute(0).withSecond(0).withNano(0);

        var tracking = rateLimitRepository
            .findByConsumerIdAndWindowStart(consumerId, windowStart)
            .orElse(RateLimitTrackingEntity.builder()
                .consumerId(consumerId)
                .windowStart(windowStart)
                .requestCount(0)
                .build());

        tracking.setRequestCount(tracking.getRequestCount() + 1);
        tracking.setLastRequest(timestamp);

        rateLimitRepository.save(tracking);
    }

    @Override
    public String getPolicyType() {
        return "RATE_LIMIT";
    }
}

