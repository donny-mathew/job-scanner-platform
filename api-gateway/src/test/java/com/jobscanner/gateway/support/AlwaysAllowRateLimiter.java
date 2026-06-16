package com.jobscanner.gateway.support;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * No-op rate limiter that always allows requests — used in integration tests to avoid Redis.
 */
public class AlwaysAllowRateLimiter extends AbstractRateLimiter<RedisRateLimiter.Config> {

    public AlwaysAllowRateLimiter() {
        super(RedisRateLimiter.Config.class, "always-allow", null);
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return Mono.just(new Response(true, Map.of()));
    }
}
