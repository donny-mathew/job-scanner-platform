package com.jobscanner.search.integration;

import com.jobscanner.search.adapter.out.search.InMemoryJobIndex;
import com.jobscanner.search.adapter.out.security.TenantContext;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.value.SearchQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical isolation test: tenant A searches must NEVER return tenant B's documents,
 * even with identical search terms and job IDs.
 */
class TenantIsolationSearchIT {

    InMemoryJobIndex index = new InMemoryJobIndex();
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    private JobSearchDocument doc(UUID id, UUID tenantId, String title) {
        return new JobSearchDocument(id, tenantId, title, "Acme", "Sydney",
                "https://example.com", "mock", OffsetDateTime.now(), 80,
                "Great match", "mock", OffsetDateTime.now());
    }

    @Test
    void tenantA_search_neverReturnsTenantB_jobs_withIdenticalSearchTerms() {
        UUID sharedJobId = UUID.randomUUID();
        // Both tenants have a job with "Java Engineer" title but different tenantId
        index.index(doc(sharedJobId, tenantA, "Java Engineer"));
        UUID jobBId = UUID.randomUUID();
        index.index(doc(jobBId, tenantB, "Java Engineer"));

        TenantContext.set(tenantA);
        List<JobSearchDocument> results = index.search(SearchQuery.of("Java", null, null, null, "score", 0, 20));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).tenantId()).isEqualTo(tenantA);
        assertThat(results).noneMatch(d -> d.tenantId().equals(tenantB));
    }

    @Test
    void tenantA_getById_cannotAccessTenantB_jobById() {
        UUID jobBId = UUID.randomUUID();
        index.index(doc(jobBId, tenantB, "Python Developer"));

        TenantContext.set(tenantA);
        Optional<JobSearchDocument> result = index.getById(jobBId);

        assertThat(result).isEmpty();
    }

    @Test
    void eachTenant_seesOnlyOwnJobs_withNoSearchFilters() {
        for (int i = 0; i < 3; i++) {
            index.index(doc(UUID.randomUUID(), tenantA, "Dev " + i));
            index.index(doc(UUID.randomUUID(), tenantB, "Dev " + i));
        }

        TenantContext.set(tenantA);
        List<JobSearchDocument> resultsA = index.search(SearchQuery.of(null, null, null, null, "score", 0, 100));
        assertThat(resultsA).hasSize(3);
        assertThat(resultsA).allMatch(d -> d.tenantId().equals(tenantA));

        TenantContext.clear();
        TenantContext.set(tenantB);
        List<JobSearchDocument> resultsB = index.search(SearchQuery.of(null, null, null, null, "score", 0, 100));
        assertThat(resultsB).hasSize(3);
        assertThat(resultsB).allMatch(d -> d.tenantId().equals(tenantB));
    }

    @Test
    void jobScoredEventFlow_documentBecomesSearchable() {
        UUID jobId = UUID.randomUUID();
        JobSearchDocument document = doc(jobId, tenantA, "Spring Boot Engineer");

        // Simulate what JobScoredEventConsumer does
        TenantContext.set(tenantA);
        index.index(document);

        List<JobSearchDocument> results = index.search(SearchQuery.of("Spring", null, null, null, "score", 0, 20));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(jobId);
    }
}
