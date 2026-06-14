package com.jobscanner.auth.domain.port.out;

import com.jobscanner.auth.domain.model.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(UUID id);
    Optional<Tenant> findByName(String name);
    boolean existsByName(String name);
}
