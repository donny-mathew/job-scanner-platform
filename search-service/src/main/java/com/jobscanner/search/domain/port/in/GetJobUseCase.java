package com.jobscanner.search.domain.port.in;

import com.jobscanner.search.domain.model.JobSearchDocument;

import java.util.Optional;
import java.util.UUID;

public interface GetJobUseCase {
    Optional<JobSearchDocument> getById(UUID id);
}
