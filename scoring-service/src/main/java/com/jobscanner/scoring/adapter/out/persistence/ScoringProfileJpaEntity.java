package com.jobscanner.scoring.adapter.out.persistence;

import com.jobscanner.scoring.domain.model.ScoringProfile;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scoring_profiles")
class ScoringProfileJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "profile_text", nullable = false, columnDefinition = "TEXT")
    private String profileText;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ScoringProfileJpaEntity() {}

    ScoringProfileJpaEntity(UUID id, UUID tenantId, String profileText, OffsetDateTime updatedAt) {
        this.id = id; this.tenantId = tenantId;
        this.profileText = profileText; this.updatedAt = updatedAt;
    }

    static ScoringProfileJpaEntity fromDomain(ScoringProfile p) {
        return new ScoringProfileJpaEntity(p.id(), p.tenantId(), p.profileText(), p.updatedAt());
    }

    ScoringProfile toDomain() { return new ScoringProfile(id, tenantId, profileText, updatedAt); }
    UUID getTenantId() { return tenantId; }
}
