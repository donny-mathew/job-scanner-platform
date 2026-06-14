package com.jobscanner.auth.adapter.out.persistence;

import com.jobscanner.auth.adapter.out.security.TenantContext;
import com.jobscanner.auth.domain.model.User;
import com.jobscanner.auth.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.save(UserJpaEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findByEmailAndTenantId(email, tenantId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        // Direct lookup by PK — still verify tenant ownership on the result
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.existsByEmailAndTenantId(email, tenantId);
    }
}
