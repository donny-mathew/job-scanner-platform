package com.jobscanner.scan.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SearchConfig(
        UUID id,
        UUID tenantId,
        List<String> keywords,
        String location,
        String filtersJson,
        String cronExpression,
        boolean enabled,
        OffsetDateTime createdAt
) {}
