package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.model.JobScore;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobScoreRepository {
    JobScore save(JobScore score);
    Optional<JobScore> findByJobListingId(UUID jobListingId);
    List<JobScore> findAllForCurrentTenant();
}
