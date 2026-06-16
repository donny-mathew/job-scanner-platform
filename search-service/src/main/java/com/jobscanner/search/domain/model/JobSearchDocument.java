package com.jobscanner.search.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobSearchDocument(
        UUID id,
        UUID tenantId,
        String title,
        String company,
        String location,
        String url,
        String source,
        OffsetDateTime discoveredAt,
        int score,
        String reasoning,
        String model,
        OffsetDateTime scoredAt
) {}
