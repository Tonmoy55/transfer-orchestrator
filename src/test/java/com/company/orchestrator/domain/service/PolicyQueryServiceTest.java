package com.company.orchestrator.domain.service;

import com.company.orchestrator.api.dto.PolicyDto;
import com.company.orchestrator.domain.enums.PolicyType;
import com.company.orchestrator.infrastructure.persistence.entity.PolicyEntity;
import com.company.orchestrator.infrastructure.persistence.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PolicyQueryServiceTest {

    private final PolicyRepository repository = Mockito.mock(PolicyRepository.class);
    private final PolicyQueryService service = new PolicyQueryService(repository);

    @Test
    void getAllPolicies_returnsMappedDtos() {
        PolicyEntity entity = PolicyEntity.builder()
                .id("1")
                .name("Test Policy")
                .type(PolicyType.TIME_BASED)
                .description("desc")
                .configuration("{\"k\":\"v\"}")
                .active(true)
                .build();

        when(repository.findAll()).thenReturn(List.of(entity));

        List<PolicyDto> result = service.getAllPolicies();

        assertThat(result).hasSize(1);
        PolicyDto dto = result.getFirst();
        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Test Policy");
        assertThat(dto.getType()).isEqualTo(PolicyType.TIME_BASED);
        assertThat(dto.getDescription()).isEqualTo("desc");
        assertThat(dto.getConfiguration()).isEqualTo("{\"k\":\"v\"}");
        assertThat(dto.getActive()).isTrue();
    }
}

