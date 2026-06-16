package com.jobscanner.search.adapter.in.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobScoredEventDto(
        UUID eventId,
        UUID tenantId,
        UUID jobListingId,
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
