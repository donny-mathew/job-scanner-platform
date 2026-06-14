package com.jobscanner.scoring.application.service;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.exception.ScoringProfileNotFoundException;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.in.ScoringProfileUseCase;
import com.jobscanner.scoring.domain.port.out.ScoringProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScoringProfileService implements ScoringProfileUseCase {

    private final ScoringProfileRepository repository;

    public ScoringProfileService(ScoringProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ScoringProfile upsert(String profileText) {
        Optional<ScoringProfile> existing = repository.findForCurrentTenant();
        ScoringProfile profile = existing
                .map(p -> new ScoringProfile(p.id(), p.tenantId(), profileText, OffsetDateTime.now()))
                .orElseGet(() -> new ScoringProfile(
                        UUID.randomUUID(), TenantContext.requireTenantId(),
                        profileText, OffsetDateTime.now()));
        return repository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ScoringProfile getForCurrentTenant() {
        return repository.findForCurrentTenant()
                .orElseThrow(ScoringProfileNotFoundException::new);
    }
}
