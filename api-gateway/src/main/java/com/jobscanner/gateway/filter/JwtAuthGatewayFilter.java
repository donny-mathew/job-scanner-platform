package com.jobscanner.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
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

            // Propagate verified claims to downstream services
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-Tenant-Id", jwt.getClaim("tenant_id").asString())
                            .header("X-User-Id", jwt.getSubject())
                            .header("X-User-Role", jwt.getClaim("role").asString()))
                    .build();

            return chain.filter(mutated);
        } catch (JWTVerificationException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
