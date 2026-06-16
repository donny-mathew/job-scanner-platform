package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.value.JobListingDto;

public interface JobScoredEventPort {
    void publish(JobScore score, JobListingDto listing);
}
