package com.jobscanner.search.domain.port.in;

import com.jobscanner.search.domain.model.JobSearchDocument;

public interface IndexJobUseCase {
    void index(JobSearchDocument document);
}
