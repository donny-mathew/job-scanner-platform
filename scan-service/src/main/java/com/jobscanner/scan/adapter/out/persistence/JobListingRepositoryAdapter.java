package com.jobscanner.scan.adapter.out.persistence;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.port.out.JobListingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JobListingRepositoryAdapter implements JobListingRepository {

    private final JobListingJpaRepository jpa;

    public JobListingRepositoryAdapter(JobListingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public JobListing save(JobListing listing) {
        return jpa.save(JobListingJpaEntity.fromDomain(listing)).toDomain();
    }

    @Override
    public Optional<JobListing> findByExternalId(String externalId) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findByExternalIdAndTenantId(externalId, tenantId)
                .map(JobListingJpaEntity::toDomain);
    }

    @Override
    public Optional<JobListing> findById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(JobListingJpaEntity::toDomain);
    }

    @Override
    public List<JobListing> findAllForCurrentTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findAllByTenantId(tenantId).stream()
                .map(JobListingJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.existsByExternalIdAndTenantId(externalId, tenantId);
    }

    @Override
    public Optional<JobListing> findByIdInternal(UUID id) {
        // No TenantContext required — used by scoring-service via internal API
        return jpa.findById(id).map(JobListingJpaEntity::toDomain);
    }
}
