package com.jobscanner.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
        UUID id,
        UUID tenantId,
        String email,
        String passwordHash,
        Role role,
        OffsetDateTime createdAt
) {
    public enum Role { OWNER, MEMBER }
}
