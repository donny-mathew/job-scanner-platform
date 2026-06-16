package com.jobscanner.search.unit;

import com.jobscanner.search.adapter.out.search.InMemoryJobIndex;
import com.jobscanner.search.adapter.out.security.TenantContext;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.value.SearchQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class InMemoryJobIndexTest {

    InMemoryJobIndex index = new InMemoryJobIndex();
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void setUp() { TenantContext.set(tenantA); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    private JobSearchDocument doc(UUID id, UUID tenantId, String title, String company,
                                   String location, String source, int score) {
        return new JobSearchDocument(id, tenantId, title, company, location,
                "https://example.com/" + id, source, OffsetDateTime.now().minusDays(1),
                score, "Good match", "mock", OffsetDateTime.now());
    }

    @Test
    void search_alwaysAppliesTenantFilter() {
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();
        index.index(doc(jobA, tenantA, "Java Engineer", "Acme", "Sydney", "mock", 80));
        index.index(doc(jobB, tenantB, "Java Engineer", "Betacorp", "Melbourne", "mock", 90));

        SearchQuery query = SearchQuery.of("Java", null, null, null, "score", 0, 20);
        List<JobSearchDocument> results = index.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(jobA);
        assertThat(results).noneMatch(d -> d.tenantId().equals(tenantB));
    }

    @Test
    void search_withNoTenantContext_throwsIllegalState() {
        TenantContext.clear();
        SearchQuery query = SearchQuery.of(null, null, null, null, "score", 0, 20);
        assertThatThrownBy(() -> index.search(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context active");
    }

    @Test
    void search_textFilter_matchesTitleAndCompany() {
        index.index(doc(UUID.randomUUID(), tenantA, "Senior Java Developer", "Acme", "Sydney", "mock", 70));
        index.index(doc(UUID.randomUUID(), tenantA, "Python Engineer", "Acme Corp", "Melbourne", "mock", 65));

        List<JobSearchDocument> results = index.search(SearchQuery.of("java", null, null, null, "score", 0, 20));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).contains("Java");
    }

    @Test
    void search_locationFilter_exactMatch() {
        index.index(doc(UUID.randomUUID(), tenantA, "Dev", "A", "Sydney", "mock", 80));
        index.index(doc(UUID.randomUUID(), tenantA, "Dev", "B", "Melbourne", "mock", 75));

        List<JobSearchDocument> results = index.search(SearchQuery.of(null, "Sydney", null, null, "score", 0, 20));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).location()).isEqualTo("Sydney");
    }

    @Test
    void search_minScoreFilter_appliesThreshold() {
        index.index(doc(UUID.randomUUID(), tenantA, "Dev", "A", "Sydney", "mock", 80));
        index.index(doc(UUID.randomUUID(), tenantA, "Dev", "B", "Sydney", "mock", 40));

        List<JobSearchDocument> results = index.search(SearchQuery.of(null, null, null, 70, "score", 0, 20));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).score()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void search_sortByScore_descendingOrder() {
        index.index(doc(UUID.randomUUID(), tenantA, "Dev A", "X", "Sydney", "mock", 60));
        index.index(doc(UUID.randomUUID(), tenantA, "Dev B", "X", "Sydney", "mock", 90));
        index.index(doc(UUID.randomUUID(), tenantA, "Dev C", "X", "Sydney", "mock", 75));

        List<JobSearchDocument> results = index.search(SearchQuery.of(null, null, null, null, "score", 0, 20));
        assertThat(results.get(0).score()).isEqualTo(90);
        assertThat(results.get(1).score()).isEqualTo(75);
        assertThat(results.get(2).score()).isEqualTo(60);
    }

    @Test
    void search_pagination_returnsCorrectPage() {
        for (int i = 0; i < 5; i++) {
            index.index(doc(UUID.randomUUID(), tenantA, "Dev " + i, "X", "Sydney", "mock", 50 + i));
        }

        List<JobSearchDocument> page0 = index.search(SearchQuery.of(null, null, null, null, "score", 0, 2));
        List<JobSearchDocument> page1 = index.search(SearchQuery.of(null, null, null, null, "score", 1, 2));
        assertThat(page0).hasSize(2);
        assertThat(page1).hasSize(2);
        assertThat(page0).doesNotContainAnyElementsOf(page1);
    }

    @Test
    void index_idempotent_reIndexUpdatesDocument() {
        UUID jobId = UUID.randomUUID();
        index.index(doc(jobId, tenantA, "Original Title", "X", "Sydney", "mock", 70));
        index.index(doc(jobId, tenantA, "Updated Title", "X", "Sydney", "mock", 85));

        Optional<JobSearchDocument> result = index.getById(jobId);
        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Updated Title");
        assertThat(result.get().score()).isEqualTo(85);
    }

    @Test
    void getById_crossTenant_returnsEmpty() {
        UUID jobId = UUID.randomUUID();
        index.index(doc(jobId, tenantB, "Dev", "X", "Sydney", "mock", 80));

        // TenantA tries to access TenantB's document
        Optional<JobSearchDocument> result = index.getById(jobId);
        assertThat(result).isEmpty();
    }
}
