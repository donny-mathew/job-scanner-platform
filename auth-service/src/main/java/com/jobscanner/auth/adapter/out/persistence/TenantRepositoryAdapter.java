package com.jobscanner.auth.adapter.out.persistence;

import com.jobscanner.auth.domain.model.Tenant;
import com.jobscanner.auth.domain.port.out.TenantRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository jpa;

    public TenantRepositoryAdapter(TenantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Tenant save(Tenant tenant) {
        return jpa.save(TenantJpaEntity.fromDomain(tenant)).toDomain();
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return jpa.findById(id).map(TenantJpaEntity::toDomain);
    }

    @Override
    public Optional<Tenant> findByName(String name) {
        return jpa.findByName(name).map(TenantJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }
}
