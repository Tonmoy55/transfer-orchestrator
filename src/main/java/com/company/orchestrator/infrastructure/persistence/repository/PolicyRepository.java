package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.domain.enums.PolicyType;
import com.company.orchestrator.infrastructure.persistence.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Policy entities
 */
@Repository
public interface PolicyRepository extends JpaRepository<PolicyEntity, String> {

    Optional<PolicyEntity> findByName(String name);

    List<PolicyEntity> findByActiveTrue();

    List<PolicyEntity> findByType(PolicyType type);
}

