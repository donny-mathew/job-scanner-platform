package com.jobscanner.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jobscanner.gateway.filter.TenantRateLimiterKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    TenantRateLimiterKeyResolver resolver = new TenantRateLimiterKeyResolver();

    private String buildJwt(UUID tenantId) {
        return JWT.create()
                .withIssuer("job-scanner-auth")
                .withSubject(UUID.randomUUID().toString())
                .withClaim("tenant_id", tenantId.toString())
                .withClaim("role", "OWNER")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256("test-secret-at-least-32-chars-long!!"));
    }

    @Test
    void resolve_withValidJwt_returnsTenantKey() {
        UUID tenantId = UUID.randomUUID();
        String jwt = buildJwt(tenantId);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();
        assertThat(key).isEqualTo("tenant:" + tenantId);
    }

    @Test
    void resolve_withoutJwt_returnsIpKey() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();
        assertThat(key).startsWith("ip:");
    }

    @Test
    void resolve_differentTenants_returnsDifferentKeys() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        MockServerWebExchange ex1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/jobs/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt(tenant1)).build());
        MockServerWebExchange ex2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/jobs/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt(tenant2)).build());

        String key1 = resolver.resolve(ex1).block();
        String key2 = resolver.resolve(ex2).block();

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).contains(tenant1.toString());
        assertThat(key2).contains(tenant2.toString());
    }

    @Test
    void resolve_malformedJwt_returnsIpFallback() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();
        assertThat(key).startsWith("ip:");
    }
}
