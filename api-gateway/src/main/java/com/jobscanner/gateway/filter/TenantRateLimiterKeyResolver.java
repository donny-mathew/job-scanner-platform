package com.jobscanner.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts the tenant_id from the JWT to use as the rate-limit key.
 * Each tenant has an independent token bucket — one noisy tenant cannot starve others.
 * Falls back to the remote IP when no valid JWT is present (covers public/unauthenticated routes).
 */
@Component("tenantRateLimiterKeyResolver")
public class TenantRateLimiterKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                DecodedJWT jwt = JWT.decode(header.substring(7));
                String tenantId = jwt.getClaim("tenant_id").asString();
                if (tenantId != null && !tenantId.isBlank()) {
                    return Mono.just("tenant:" + tenantId);
                }
            } catch (Exception ignored) {}
        }
        // Fallback: use remote IP
        String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return Mono.just("ip:" + remoteAddr);
    }
}
