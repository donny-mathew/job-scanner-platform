package com.jobscanner.scan.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface JobListingJpaRepository extends JpaRepository<JobListingJpaEntity, UUID> {
    Optional<JobListingJpaEntity> findByExternalIdAndTenantId(String externalId, UUID tenantId);
    boolean existsByExternalIdAndTenantId(String externalId, UUID tenantId);
    List<JobListingJpaEntity> findAllByTenantId(UUID tenantId);
}
