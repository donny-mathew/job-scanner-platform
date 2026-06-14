package com.jobscanner.auth.adapter.in.web.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID tenantId,
        UUID userId
) {}
