package com.jobscanner.search.adapter.out.search;

import com.jobscanner.search.adapter.out.security.TenantContext;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import com.jobscanner.search.domain.value.SearchQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory index for local development and tests — no OpenSearch required.
 * Tenant isolation is structural: every operation reads from TenantContext.requireTenantId().
 */
public class InMemoryJobIndex implements JobSearchIndex {

    // tenantId → (docId → document)
    private final Map<UUID, Map<UUID, JobSearchDocument>> store = new ConcurrentHashMap<>();

    @Override
    public void index(JobSearchDocument document) {
        store.computeIfAbsent(document.tenantId(), k -> new ConcurrentHashMap<>())
                .put(document.id(), document);
    }

    @Override
    public List<JobSearchDocument> search(SearchQuery query) {
        UUID tenantId = TenantContext.requireTenantId();
        Map<UUID, JobSearchDocument> tenantDocs = store.getOrDefault(tenantId, Map.of());
        return tenantDocs.values().stream()
                .filter(d -> matchesText(d, query.text()))
                .filter(d -> matchesLocation(d, query.location()))
                .filter(d -> matchesSource(d, query.source()))
                .filter(d -> matchesMinScore(d, query.minScore()))
                .sorted(comparatorFor(query.sortBy()))
                .skip((long) query.page() * query.size())
                .limit(query.size())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JobSearchDocument> getById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return Optional.ofNullable(store.getOrDefault(tenantId, Map.of()).get(id));
    }

    @Override
    public void deleteById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        store.getOrDefault(tenantId, Map.of()).remove(id);
    }

    private boolean matchesText(JobSearchDocument d, String text) {
        if (text == null || text.isBlank()) return true;
        String lower = text.toLowerCase();
        return (d.title() != null && d.title().toLowerCase().contains(lower))
                || (d.company() != null && d.company().toLowerCase().contains(lower));
    }

    private boolean matchesLocation(JobSearchDocument d, String location) {
        if (location == null || location.isBlank()) return true;
        return location.equalsIgnoreCase(d.location());
    }

    private boolean matchesSource(JobSearchDocument d, String source) {
        if (source == null || source.isBlank()) return true;
        return source.equalsIgnoreCase(d.source());
    }

    private boolean matchesMinScore(JobSearchDocument d, Integer minScore) {
        if (minScore == null) return true;
        return d.score() >= minScore;
    }

    private Comparator<JobSearchDocument> comparatorFor(SearchQuery.SortField sort) {
        if (sort == SearchQuery.SortField.DISCOVERED_AT) {
            return Comparator.comparing(JobSearchDocument::discoveredAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparingInt(JobSearchDocument::score).reversed();
    }
}
