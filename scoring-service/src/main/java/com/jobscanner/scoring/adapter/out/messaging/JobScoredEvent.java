package com.jobscanner.scoring.adapter.out.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobScoredEvent(
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
