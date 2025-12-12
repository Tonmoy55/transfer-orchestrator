package com.company.orchestrator.infrastructure.edc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EDC Contract Offer model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractOffer {
    private String offerId;
    private String assetId;
    private String providerId;
    private String consumerId;
    private String policyId;
}


