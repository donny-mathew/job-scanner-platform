package com.jobscanner.search.application.service;

import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.in.IndexJobUseCase;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexJobService implements IndexJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(IndexJobService.class);

    private final JobSearchIndex index;

    public IndexJobService(JobSearchIndex index) {
        this.index = index;
    }

    @Override
    public void index(JobSearchDocument document) {
        log.info("[tenant={}] Indexing job {}", document.tenantId(), document.id());
        index.index(document);
    }
}
