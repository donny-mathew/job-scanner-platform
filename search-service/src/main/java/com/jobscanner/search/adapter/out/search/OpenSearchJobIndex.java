package com.jobscanner.search.adapter.out.search;

import com.jobscanner.search.adapter.out.security.TenantContext;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import com.jobscanner.search.domain.value.SearchQuery;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OpenSearchJobIndex implements JobSearchIndex {

    static final String INDEX = "job_listings";
    private static final Logger log = LoggerFactory.getLogger(OpenSearchJobIndex.class);

    private final OpenSearchClient client;

    public OpenSearchJobIndex(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void index(JobSearchDocument document) {
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(document.id().toString())
                    .document(document));
        } catch (IOException e) {
            throw new RuntimeException("Failed to index document " + document.id(), e);
        }
    }

    @Override
    public List<JobSearchDocument> search(SearchQuery query) {
        UUID tenantId = TenantContext.requireTenantId();
        try {
            List<Query> filters = new ArrayList<>();
            // Tenant filter is always applied — impossible to bypass
            filters.add(Query.of(q -> q.term(t -> t
                    .field("tenantId")
                    .value(FieldValue.of(v -> v.stringValue(tenantId.toString()))))));

            if (query.location() != null && !query.location().isBlank()) {
                String loc = query.location();
                filters.add(Query.of(q -> q.term(t -> t
                        .field("location")
                        .value(FieldValue.of(v -> v.stringValue(loc))))));
            }
            if (query.source() != null && !query.source().isBlank()) {
                String src = query.source();
                filters.add(Query.of(q -> q.term(t -> t
                        .field("source")
                        .value(FieldValue.of(v -> v.stringValue(src))))));
            }
            if (query.minScore() != null) {
                int min = query.minScore();
                filters.add(Query.of(q -> q.range(r -> r
                        .field("score")
                        .gte(JsonData.of(min)))));
            }

            Query mainQuery;
            if (query.text() != null && !query.text().isBlank()) {
                String text = query.text();
                Query textQuery = Query.of(q -> q.multiMatch(m -> m
                        .query(text)
                        .fields(List.of("title", "company"))));
                mainQuery = Query.of(q -> q.bool(b -> b.must(textQuery).filter(filters)));
            } else {
                mainQuery = Query.of(q -> q.bool(b -> b.filter(filters)));
            }

            SortOptions sort = buildSort(query.sortBy());
            int from = query.page() * query.size();

            SearchResponse<JobSearchDocument> response = client.search(s -> s
                    .index(INDEX)
                    .query(mainQuery)
                    .sort(sort)
                    .from(from)
                    .size(query.size()), JobSearchDocument.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("[tenant={}] OpenSearch search failed: {}", tenantId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<JobSearchDocument> getById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        try {
            GetResponse<JobSearchDocument> response = client.get(g -> g
                    .index(INDEX)
                    .id(id.toString()), JobSearchDocument.class);
            if (!response.found()) return Optional.empty();
            JobSearchDocument doc = response.source();
            // Enforce tenant isolation — reject cross-tenant access by construction
            if (doc == null || !tenantId.equals(doc.tenantId())) return Optional.empty();
            return Optional.of(doc);
        } catch (IOException e) {
            log.error("[tenant={}] Failed to fetch job {} from OpenSearch: {}", tenantId, id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteById(UUID id) {
        try {
            client.delete(d -> d.index(INDEX).id(id.toString()));
        } catch (IOException e) {
            log.error("Failed to delete document {} from OpenSearch: {}", id, e.getMessage());
        }
    }

    private SortOptions buildSort(SearchQuery.SortField sortField) {
        String field = sortField == SearchQuery.SortField.DISCOVERED_AT ? "discoveredAt" : "score";
        return SortOptions.of(s -> s.field(FieldSort.of(f -> f.field(field).order(SortOrder.Desc))));
    }
}
