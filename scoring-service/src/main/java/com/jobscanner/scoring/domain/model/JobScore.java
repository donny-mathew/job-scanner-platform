package com.jobscanner.scoring.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobScore(
        UUID id,
        UUID tenantId,
        UUID jobListingId,
        int score,
        String reasoning,
        String model,
        OffsetDateTime scoredAt
) {}
