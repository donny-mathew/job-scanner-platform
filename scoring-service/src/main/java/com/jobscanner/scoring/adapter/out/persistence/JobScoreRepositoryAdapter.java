package com.jobscanner.scoring.adapter.out.persistence;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.port.out.JobScoreRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JobScoreRepositoryAdapter implements JobScoreRepository {

    private final JobScoreJpaRepository jpa;

    public JobScoreRepositoryAdapter(JobScoreJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public JobScore save(JobScore score) {
        return jpa.save(JobScoreJpaEntity.fromDomain(score)).toDomain();
    }

    @Override
    public Optional<JobScore> findByJobListingId(UUID jobListingId) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findByJobListingIdAndTenantId(jobListingId, tenantId)
                .map(JobScoreJpaEntity::toDomain);
    }

    @Override
    public List<JobScore> findAllForCurrentTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findAllByTenantId(tenantId).stream()
                .map(JobScoreJpaEntity::toDomain).toList();
    }
}
