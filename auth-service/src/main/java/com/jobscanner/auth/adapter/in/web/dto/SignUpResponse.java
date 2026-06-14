package com.jobscanner.auth.adapter.in.web.dto;

import java.util.UUID;

public record SignUpResponse(
        UUID userId,
        UUID tenantId,
        String tenantName,
        String token
) {}
