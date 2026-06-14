package com.jobscanner.scan.domain.value;

public record RawJob(
        String externalId,
        String title,
        String company,
        String location,
        String url,
        String rawPayload,
        String source
) {}
