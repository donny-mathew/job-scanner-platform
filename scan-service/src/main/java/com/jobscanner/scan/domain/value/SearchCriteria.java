package com.jobscanner.scan.domain.value;

import java.util.List;

public record SearchCriteria(
        List<String> keywords,
        String location,
        boolean aiVisaSponsorshipOnly
) {}
