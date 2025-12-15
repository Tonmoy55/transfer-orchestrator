package com.company.orchestrator.api.dto;

import com.company.orchestrator.domain.enums.PolicyType;
import lombok.Builder;
import lombok.Value;

/**
 * API DTO representing a Policy definition.
 * Exposes all columns from {@code PolicyEntity} to API consumers.
 */
@Value
@Builder
public class PolicyDto {

    String id;
    String name;
    PolicyType type;
    String description;
    String configuration;
    Boolean active;
}

