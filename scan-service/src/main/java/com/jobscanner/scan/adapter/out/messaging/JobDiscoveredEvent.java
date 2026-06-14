package com.jobscanner.scan.adapter.out.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobDiscoveredEvent(
        UUID eventId,
        UUID tenantId,
        UUID jobListingId,
        String externalId,
        String title,
        String source,
        OffsetDateTime discoveredAt
) {}
