package com.jobscanner.search.domain.port.in;

import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.value.SearchQuery;

import java.util.List;

public interface SearchJobUseCase {
    List<JobSearchDocument> search(SearchQuery query);
}
