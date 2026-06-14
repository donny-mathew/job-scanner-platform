package com.jobscanner.auth.adapter.out.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        HOLDER.set(tenantId);
    }

    public static UUID get() {
        return HOLDER.get();
    }

    /**
     * Returns the current tenant ID or throws if the context is not set.
     * Called by repository adapters to enforce that all queries are tenant-scoped.
     */
    public static UUID requireTenantId() {
        UUID id = HOLDER.get();
        if (id == null) {
            throw new IllegalStateException(
                    "No tenant context active — all repository calls must run within a tenant scope");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
