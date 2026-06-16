package com.jobscanner.gateway.config;

import com.jobscanner.gateway.filter.JwtAuthGatewayFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final JwtAuthGatewayFilter jwtAuthFilter;
    private final RateLimiter<?> rateLimiter;

    @Value("${app.routes.auth-service}")
    private String authServiceUrl;

    @Value("${app.routes.scan-service}")
    private String scanServiceUrl;

    @Value("${app.routes.scoring-service}")
    private String scoringServiceUrl;

    @Value("${app.routes.search-service}")
    private String searchServiceUrl;

    public RouteConfig(JwtAuthGatewayFilter jwtAuthFilter, RateLimiter<?> rateLimiter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimiter = rateLimiter;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RouteLocator routes(RouteLocatorBuilder builder, KeyResolver tenantRateLimiterKeyResolver) {
        return builder.routes()
                // Public: auth endpoints (no JWT required)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter((RateLimiter<Object>) rateLimiter)
                                .setKeyResolver(tenantRateLimiterKeyResolver)))
                        .uri(authServiceUrl))

                // Protected: search / job results
                .route("search-service", r -> r
                        .path("/api/v1/jobs/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter((RateLimiter<Object>) rateLimiter)
                                        .setKeyResolver(tenantRateLimiterKeyResolver)))
                        .uri(searchServiceUrl))

                // Protected: scan management
                .route("scan-service", r -> r
                        .path("/api/v1/scans/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter((RateLimiter<Object>) rateLimiter)
                                        .setKeyResolver(tenantRateLimiterKeyResolver)))
                        .uri(scanServiceUrl))

                // Protected: scoring / profiles
                .route("scoring-service", r -> r
                        .path("/api/v1/scores/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter((RateLimiter<Object>) rateLimiter)
                                        .setKeyResolver(tenantRateLimiterKeyResolver)))
                        .uri(scoringServiceUrl))

                .build();
    }
}
