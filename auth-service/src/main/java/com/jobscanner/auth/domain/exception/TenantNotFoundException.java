package com.jobscanner.auth.domain.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found: " + tenantId);
    }
    public TenantNotFoundException(String name) {
        super("Tenant not found: " + name);
    }
}
