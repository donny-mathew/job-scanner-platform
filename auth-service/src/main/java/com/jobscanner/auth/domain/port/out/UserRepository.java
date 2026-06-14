package com.jobscanner.auth.domain.port.out;

import com.jobscanner.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    /**
     * Finds a user by email scoped to the tenant in TenantContext.
     * Callers must not supply tenantId — it is resolved automatically.
     */
    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);
}
