package com.jobscanner.scoring.adapter.out.persistence;

import com.jobscanner.scoring.domain.model.JobScore;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_scores")
class JobScoreJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_listing_id", nullable = false)
    private UUID jobListingId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Column(nullable = false)
    private String model;

    @Column(name = "scored_at", nullable = false)
    private OffsetDateTime scoredAt;

    protected JobScoreJpaEntity() {}

    JobScoreJpaEntity(UUID id, UUID tenantId, UUID jobListingId, int score,
                      String reasoning, String model, OffsetDateTime scoredAt) {
        this.id = id; this.tenantId = tenantId; this.jobListingId = jobListingId;
        this.score = score; this.reasoning = reasoning; this.model = model; this.scoredAt = scoredAt;
    }

    static JobScoreJpaEntity fromDomain(JobScore s) {
        return new JobScoreJpaEntity(s.id(), s.tenantId(), s.jobListingId(),
                s.score(), s.reasoning(), s.model(), s.scoredAt());
    }

    JobScore toDomain() {
        return new JobScore(id, tenantId, jobListingId, score, reasoning, model, scoredAt);
    }

    UUID getTenantId() { return tenantId; }
    UUID getJobListingId() { return jobListingId; }
}
