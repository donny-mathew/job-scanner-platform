package com.jobscanner.scan.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobListing(
        UUID id,
        UUID tenantId,
        String externalId,
        String title,
        String company,
        String location,
        String url,
        String rawPayload,
        String source,
        OffsetDateTime discoveredAt
) {}
