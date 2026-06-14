package com.jobscanner.auth.adapter.out.persistence;

import com.jobscanner.auth.domain.model.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private User.Role role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserJpaEntity() {}

    UserJpaEntity(UUID id, UUID tenantId, String email, String passwordHash,
                  User.Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    static UserJpaEntity fromDomain(User user) {
        return new UserJpaEntity(
                user.id(), user.tenantId(), user.email(),
                user.passwordHash(), user.role(), user.createdAt()
        );
    }

    User toDomain() {
        return new User(id, tenantId, email, passwordHash, role, createdAt);
    }

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
    String getEmail() { return email; }
}
