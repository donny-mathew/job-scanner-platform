package com.jobscanner.scoring.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScoringProfile(UUID id, UUID tenantId, String profileText, OffsetDateTime updatedAt) {}
