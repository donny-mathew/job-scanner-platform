package com.jobscanner.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthGatewayFilter implements GatewayFilter {

    private final Algorithm algorithm;

    public JwtAuthGatewayFilter(@Value("${app.jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer("job-scanner-auth")
                    .build()
                    .verify(header.substring(7));

            String tenantId = jwt.getClaim("tenant_id").asString();
            String userId = jwt.getSubject();
            String role = jwt.getClaim("role").asString();

            // Build a mutable copy of headers and inject verified claims.
            // ServerHttpRequestDecorator is used because DefaultServerHttpRequestBuilder
            // exposes ReadOnlyHttpHeaders to mutation lambdas, causing UnsupportedOperationException.
            ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.addAll(super.getHeaders());
                    headers.set("X-Tenant-Id", tenantId);
                    headers.set("X-User-Id", userId);
                    headers.set("X-User-Role", role);
                    return headers;
                }
            };

            return chain.filter(exchange.mutate().request(decorated).build());
        } catch (JWTVerificationException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
