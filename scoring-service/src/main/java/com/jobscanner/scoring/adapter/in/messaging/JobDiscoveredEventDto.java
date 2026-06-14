package com.jobscanner.scoring.adapter.in.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobDiscoveredEventDto(
        UUID eventId,
        UUID tenantId,
        UUID jobListingId,
        String externalId,
        String title,
        String source,
        OffsetDateTime discoveredAt
) {
    public JobDiscoveredEventDto() {
        this(null, null, null, null, null, null, null);
    }
}
