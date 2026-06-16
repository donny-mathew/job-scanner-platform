package com.jobscanner.search.adapter.in.web.dto;

import com.jobscanner.search.domain.model.JobSearchDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobSearchResponse(
        UUID id,
        String title,
        String company,
        String location,
        String url,
        String source,
        OffsetDateTime discoveredAt,
        int score,
        String reasoning,
        OffsetDateTime scoredAt
) {
    public static JobSearchResponse from(JobSearchDocument doc) {
        return new JobSearchResponse(
                doc.id(), doc.title(), doc.company(), doc.location(),
                doc.url(), doc.source(), doc.discoveredAt(),
                doc.score(), doc.reasoning(), doc.scoredAt());
    }
}
