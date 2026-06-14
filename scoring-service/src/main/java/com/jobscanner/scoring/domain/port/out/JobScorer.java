package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;

public interface JobScorer {
    ScoreResult score(ScoringProfile profile, JobListingDto listing);
}
