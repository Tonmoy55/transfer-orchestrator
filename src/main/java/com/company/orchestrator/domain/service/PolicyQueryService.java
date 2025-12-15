package com.company.orchestrator.domain.service;

import com.company.orchestrator.api.dto.PolicyDto;
import com.company.orchestrator.infrastructure.persistence.entity.PolicyEntity;
import com.company.orchestrator.infrastructure.persistence.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only service for querying policy definitions.
 */
@Service
@RequiredArgsConstructor
public class PolicyQueryService {

    private final PolicyRepository policyRepository;

    public List<PolicyDto> getAllPolicies() {
        return policyRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PolicyDto toDto(PolicyEntity entity) {
        return PolicyDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .description(entity.getDescription())
                .configuration(entity.getConfiguration())
                .active(entity.getActive())
                .build();
    }
}

