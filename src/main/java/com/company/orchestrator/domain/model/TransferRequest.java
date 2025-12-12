package com.company.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Transfer request model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String consumerId;
    private String providerId;
    private String assetId;
    private String dataType;
    private List<String> policyIds;
    private String consumerRegion;
    private String consumerCertificationLevel;
    private String usagePurpose;
}

