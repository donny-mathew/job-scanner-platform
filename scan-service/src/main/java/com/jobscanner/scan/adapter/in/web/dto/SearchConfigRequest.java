package com.jobscanner.scan.adapter.in.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SearchConfigRequest(
        @NotEmpty List<String> keywords,
        String location,
        String filtersJson,
        String cronExpression,
        boolean enabled
) {}
