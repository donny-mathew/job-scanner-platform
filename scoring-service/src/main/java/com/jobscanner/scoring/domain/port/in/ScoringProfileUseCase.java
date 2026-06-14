package com.jobscanner.scoring.domain.port.in;

import com.jobscanner.scoring.domain.model.ScoringProfile;

public interface ScoringProfileUseCase {
    ScoringProfile upsert(String profileText);
    ScoringProfile getForCurrentTenant();
}
