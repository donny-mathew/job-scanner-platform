package com.jobscanner.scan.adapter.out.persistence;

import com.jobscanner.scan.domain.model.SearchConfig;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "search_configs")
class SearchConfigJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> keywords;

    @Column
    private String location;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters_json", nullable = false, columnDefinition = "jsonb")
    private String filtersJson;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SearchConfigJpaEntity() {}

    SearchConfigJpaEntity(UUID id, UUID tenantId, List<String> keywords, String location,
                          String filtersJson, String cronExpression, boolean enabled,
                          OffsetDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.keywords = keywords;
        this.location = location;
        this.filtersJson = filtersJson;
        this.cronExpression = cronExpression;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    static SearchConfigJpaEntity fromDomain(SearchConfig c) {
        return new SearchConfigJpaEntity(c.id(), c.tenantId(), c.keywords(), c.location(),
                c.filtersJson(), c.cronExpression(), c.enabled(), c.createdAt());
    }

    SearchConfig toDomain() {
        return new SearchConfig(id, tenantId, keywords, location, filtersJson,
                cronExpression, enabled, createdAt);
    }

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
    boolean isEnabled() { return enabled; }
}
