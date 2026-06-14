package com.jobscanner.scoring.adapter.out.security;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> HOLDER = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(UUID tenantId) { HOLDER.set(tenantId); }
    public static UUID get() { return HOLDER.get(); }
    public static UUID requireTenantId() {
        UUID id = HOLDER.get();
        if (id == null) throw new IllegalStateException("No tenant context active");
        return id;
    }
    public static void clear() { HOLDER.remove(); }
}
