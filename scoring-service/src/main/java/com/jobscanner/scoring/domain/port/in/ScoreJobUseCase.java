package com.jobscanner.scoring.domain.port.in;

import com.jobscanner.scoring.domain.model.JobScore;

import java.util.UUID;

public interface ScoreJobUseCase {
    JobScore scoreJob(UUID tenantId, UUID jobListingId);
}
