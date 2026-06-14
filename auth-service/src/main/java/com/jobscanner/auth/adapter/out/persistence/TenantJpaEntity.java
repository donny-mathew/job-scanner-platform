package com.jobscanner.auth.adapter.out.persistence;

import com.jobscanner.auth.domain.model.Tenant;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
class TenantJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TenantJpaEntity() {}

    TenantJpaEntity(UUID id, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    static TenantJpaEntity fromDomain(Tenant tenant) {
        return new TenantJpaEntity(tenant.id(), tenant.name(), tenant.createdAt());
    }

    Tenant toDomain() {
        return new Tenant(id, name, createdAt);
    }

    UUID getId() { return id; }
    String getName() { return name; }
    OffsetDateTime getCreatedAt() { return createdAt; }
}
