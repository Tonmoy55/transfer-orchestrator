package com.company.orchestrator.api;

import com.company.orchestrator.api.dto.ApiResponse;
import com.company.orchestrator.api.dto.PolicyDto;
import com.company.orchestrator.api.util.ResponseEntityBuilder;
import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.service.PolicyEvaluationService;
import com.company.orchestrator.domain.service.PolicyQueryService;
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
    private final PolicyQueryService policyQueryService;

    /**
     * Evaluates policies for a transfer request (test endpoint)
     */
    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<PolicyEvaluationResult>> evaluatePolicy(
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

        String message = result.isAllowed() ?
            "Policy evaluation passed" :
            "Policy evaluation failed: " + result.getReason();

        return ResponseEntityBuilder.ok(result, message);
    }

    /**
     * Lists available policy types
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getPolicyTypes() {
        log.debug("Getting available policy types");

        List<String> policyTypes = policyEvaluationService.getAvailablePolicyTypes();

        return ResponseEntityBuilder.ok(policyTypes, "Policy types retrieved successfully");
    }

    /**
     * Lists all configured policies with full details.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyDto>>> getAllPolicies() {
        log.debug("Getting all policies");

        List<PolicyDto> policies = policyQueryService.getAllPolicies();

        return ResponseEntityBuilder.ok(policies, "Policies retrieved successfully");
    }
}
