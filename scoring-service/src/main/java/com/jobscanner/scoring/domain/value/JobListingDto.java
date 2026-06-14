package com.jobscanner.scoring.domain.value;

import java.util.UUID;

public record JobListingDto(UUID id, UUID tenantId, String title, String company,
                            String location, String url, String rawPayload) {}
