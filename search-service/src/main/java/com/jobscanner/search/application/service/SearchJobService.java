package com.jobscanner.search.application.service;

import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.in.GetJobUseCase;
import com.jobscanner.search.domain.port.in.SearchJobUseCase;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import com.jobscanner.search.domain.value.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SearchJobService implements SearchJobUseCase, GetJobUseCase {

    private final JobSearchIndex index;

    public SearchJobService(JobSearchIndex index) {
        this.index = index;
    }

    @Override
    public List<JobSearchDocument> search(SearchQuery query) {
        return index.search(query);
    }

    @Override
    public Optional<JobSearchDocument> getById(UUID id) {
        return index.getById(id);
    }
}
