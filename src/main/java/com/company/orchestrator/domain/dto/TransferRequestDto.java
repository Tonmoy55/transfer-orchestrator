package com.company.orchestrator.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transfer request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto {

    @NotBlank(message = "Consumer ID is required")
    private String consumerId;

    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @NotBlank(message = "Asset ID is required")
    private String assetId;

    private String dataType;

    private List<String> policyIds;

    @NotBlank(message = "Consumer region is required")
    private String consumerRegion;

    @NotBlank(message = "Consumer certification level is required")
    private String consumerCertificationLevel;

    @NotBlank(message = "Usage purpose is required")
    private String usagePurpose;
}
