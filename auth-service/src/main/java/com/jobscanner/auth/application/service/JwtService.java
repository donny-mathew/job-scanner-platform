package com.jobscanner.auth.application.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jobscanner.auth.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_ROLE = "role";
    private static final String ISSUER = "job-scanner-auth";

    private final Algorithm algorithm;
    private final long expiryHours;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-hours:24}") long expiryHours) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiryHours = expiryHours;
    }

    public String generateToken(User user) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.id().toString())
                .withClaim(CLAIM_TENANT_ID, user.tenantId().toString())
                .withClaim(CLAIM_ROLE, user.role().name())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public DecodedJWT validateAndDecode(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("Invalid or expired token", e);
        }
    }

    public UUID extractTenantId(DecodedJWT jwt) {
        return UUID.fromString(jwt.getClaim(CLAIM_TENANT_ID).asString());
    }

    public UUID extractUserId(DecodedJWT jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
