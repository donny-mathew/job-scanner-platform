package com.jobscanner.scoring.adapter.out.persistence;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.out.ScoringProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ScoringProfileRepositoryAdapter implements ScoringProfileRepository {

    private final ScoringProfileJpaRepository jpa;

    public ScoringProfileRepositoryAdapter(ScoringProfileJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public ScoringProfile save(ScoringProfile profile) {
        return jpa.save(ScoringProfileJpaEntity.fromDomain(profile)).toDomain();
    }

    @Override
    public Optional<ScoringProfile> findForCurrentTenant() {
        return jpa.findByTenantId(TenantContext.requireTenantId())
                .map(ScoringProfileJpaEntity::toDomain);
    }

    @Override
    public Optional<ScoringProfile> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId).map(ScoringProfileJpaEntity::toDomain);
    }
}
