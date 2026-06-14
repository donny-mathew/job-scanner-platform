package com.jobscanner.scan.unit;

import com.jobscanner.scan.adapter.out.provider.MockJobProvider;
import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockJobProviderTest {

    private final MockJobProvider provider = new MockJobProvider();

    @Test
    void fetchJobs_returnsThreeCannedJobs() {
        List<RawJob> jobs = provider.fetchJobs(
                new SearchCriteria(List.of("java", "spring boot"), "Australia", false));

        assertThat(jobs).hasSize(3);
        assertThat(jobs).allMatch(j -> j.source().equals("mock"));
        assertThat(jobs).allMatch(j -> j.externalId().startsWith("mock-"));
    }

    @Test
    void fetchJobs_titleContainsFirstKeyword() {
        List<RawJob> jobs = provider.fetchJobs(
                new SearchCriteria(List.of("kotlin"), "Sydney", false));

        assertThat(jobs).allMatch(j -> j.title().toLowerCase().contains("kotlin"));
    }

    @Test
    void fetchJobs_locationReflectedInJobs() {
        List<RawJob> jobs = provider.fetchJobs(
                new SearchCriteria(List.of("java"), "Melbourne", false));

        assertThat(jobs).allMatch(j -> j.location().equals("Melbourne"));
    }

    @Test
    void fetchJobs_eachJobHasUniqueExternalId() {
        List<RawJob> jobs = provider.fetchJobs(
                new SearchCriteria(List.of("java"), "Remote", false));

        long uniqueIds = jobs.stream().map(RawJob::externalId).distinct().count();
        assertThat(uniqueIds).isEqualTo(jobs.size());
    }
}
