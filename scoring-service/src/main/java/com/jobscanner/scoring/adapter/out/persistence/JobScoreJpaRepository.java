package com.jobscanner.scoring.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface JobScoreJpaRepository extends JpaRepository<JobScoreJpaEntity, UUID> {
    Optional<JobScoreJpaEntity> findByJobListingIdAndTenantId(UUID jobListingId, UUID tenantId);
    List<JobScoreJpaEntity> findAllByTenantId(UUID tenantId);
}
