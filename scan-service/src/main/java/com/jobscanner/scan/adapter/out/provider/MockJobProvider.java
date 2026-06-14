package com.jobscanner.scan.adapter.out.provider;

import com.jobscanner.scan.domain.port.out.JobDataProvider;
import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;

import java.util.List;
import java.util.UUID;

public class MockJobProvider implements JobDataProvider {

    @Override
    public List<RawJob> fetchJobs(SearchCriteria criteria) {
        String keywordTag = criteria.keywords().isEmpty() ? "general" : criteria.keywords().get(0);
        String location = criteria.location() != null ? criteria.location() : "Remote";

        return List.of(
                new RawJob(
                        "mock-" + UUID.randomUUID(),
                        "Senior " + keywordTag + " Engineer",
                        "Acme Corp",
                        location,
                        "https://careers.acme.example/jobs/1",
                        "{\"description\": \"Great senior role with visa sponsorship\"}",
                        "mock"
                ),
                new RawJob(
                        "mock-" + UUID.randomUUID(),
                        "Lead " + keywordTag + " Developer",
                        "TechStart Pty Ltd",
                        location,
                        "https://careers.techstart.example/jobs/2",
                        "{\"description\": \"Lead developer position, permanent resident or citizen\"}",
                        "mock"
                ),
                new RawJob(
                        "mock-" + UUID.randomUUID(),
                        "Principal " + keywordTag + " Architect",
                        "BigBank AU",
                        location,
                        "https://careers.bigbank.example/jobs/3",
                        "{\"description\": \"Principal architect role in Melbourne\"}",
                        "mock"
                )
        );
    }
}
