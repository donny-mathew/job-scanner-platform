package com.jobscanner.scan.adapter.out.persistence;

import com.jobscanner.scan.domain.model.JobListing;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_listings")
class JobListingJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column
    private String location;

    @Column(nullable = false)
    private String url;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(nullable = false)
    private String source;

    @Column(name = "discovered_at", nullable = false)
    private OffsetDateTime discoveredAt;

    protected JobListingJpaEntity() {}

    JobListingJpaEntity(UUID id, UUID tenantId, String externalId, String title,
                        String company, String location, String url,
                        String rawPayload, String source, OffsetDateTime discoveredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.rawPayload = rawPayload;
        this.source = source;
        this.discoveredAt = discoveredAt;
    }

    static JobListingJpaEntity fromDomain(JobListing l) {
        return new JobListingJpaEntity(l.id(), l.tenantId(), l.externalId(), l.title(),
                l.company(), l.location(), l.url(), l.rawPayload(), l.source(), l.discoveredAt());
    }

    JobListing toDomain() {
        return new JobListing(id, tenantId, externalId, title, company, location,
                url, rawPayload, source, discoveredAt);
    }

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
}
