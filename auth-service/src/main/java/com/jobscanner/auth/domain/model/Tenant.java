package com.jobscanner.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Tenant(
        UUID id,
        String name,
        OffsetDateTime createdAt
) {}
