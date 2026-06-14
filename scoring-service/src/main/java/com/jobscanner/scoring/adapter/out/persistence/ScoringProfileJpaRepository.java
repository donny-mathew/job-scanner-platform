package com.jobscanner.scoring.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ScoringProfileJpaRepository extends JpaRepository<ScoringProfileJpaEntity, UUID> {
    Optional<ScoringProfileJpaEntity> findByTenantId(UUID tenantId);
}
