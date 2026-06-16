package com.jobscanner.search.domain.port.out;

import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.value.SearchQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobSearchIndex {
    void index(JobSearchDocument document);
    List<JobSearchDocument> search(SearchQuery query);
    Optional<JobSearchDocument> getById(UUID id);
    void deleteById(UUID id);
}
