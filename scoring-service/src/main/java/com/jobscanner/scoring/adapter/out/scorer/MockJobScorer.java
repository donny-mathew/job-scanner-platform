package com.jobscanner.scoring.adapter.out.scorer;

import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.out.JobScorer;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;

public class MockJobScorer implements JobScorer {

    @Override
    public ScoreResult score(ScoringProfile profile, JobListingDto listing) {
        return new ScoreResult(75, "Mock score — no API call made", "mock");
    }
}
