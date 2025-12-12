package com.company.orchestrator.api;

import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.service.PolicyEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Policy Operations
 */
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyEvaluationService policyEvaluationService;

    /**
     * Evaluates policies for a transfer request (test endpoint)
     */
    @PostMapping("/evaluate")
    public ResponseEntity<PolicyEvaluationResult> evaluatePolicy(
            @Valid @RequestBody TransferRequestDto requestDto) {

        log.info("Evaluating policies for test request");

        TransferRequest request = TransferRequest.builder()
            .consumerId(requestDto.getConsumerId())
            .providerId(requestDto.getProviderId())
            .assetId(requestDto.getAssetId())
            .dataType(requestDto.getDataType())
            .policyIds(requestDto.getPolicyIds())
            .consumerRegion(requestDto.getConsumerRegion())
            .consumerCertificationLevel(requestDto.getConsumerCertificationLevel())
            .usagePurpose(requestDto.getUsagePurpose())
            .build();

        PolicyEvaluationResult result = policyEvaluationService.evaluateAll(request);

        return ResponseEntity.ok(result);
    }

    /**
     * Lists available policy types
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getPolicyTypes() {
        log.debug("Getting available policy types");

        List<String> policyTypes = policyEvaluationService.getAvailablePolicyTypes();

        return ResponseEntity.ok(policyTypes);
    }
}

