package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.model.ScoringProfile;

import java.util.Optional;
import java.util.UUID;

public interface ScoringProfileRepository {
    ScoringProfile save(ScoringProfile profile);
    Optional<ScoringProfile> findForCurrentTenant();
    Optional<ScoringProfile> findByTenantId(UUID tenantId);
}
