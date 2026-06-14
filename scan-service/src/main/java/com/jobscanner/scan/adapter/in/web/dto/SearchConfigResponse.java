package com.jobscanner.scan.adapter.in.web.dto;

import com.jobscanner.scan.domain.model.SearchConfig;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SearchConfigResponse(
        UUID id,
        UUID tenantId,
        List<String> keywords,
        String location,
        String filtersJson,
        String cronExpression,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public static SearchConfigResponse from(SearchConfig c) {
        return new SearchConfigResponse(c.id(), c.tenantId(), c.keywords(), c.location(),
                c.filtersJson(), c.cronExpression(), c.enabled(), c.createdAt());
    }
}
