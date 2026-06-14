package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.value.JobListingDto;

import java.util.Optional;
import java.util.UUID;

public interface JobListingClient {
    Optional<JobListingDto> fetchJobListing(UUID jobListingId);
}
