package com.jobscanner.auth.unit;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jobscanner.auth.application.service.JwtService;
import com.jobscanner.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-characters-long";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 24);
    }

    @Test
    void generateToken_containsExpectedClaims() {
        User user = testUser(User.Role.OWNER);
        String token = jwtService.generateToken(user);

        DecodedJWT decoded = jwtService.validateAndDecode(token);

        assertThat(decoded.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwtService.extractTenantId(decoded)).isEqualTo(user.tenantId());
        assertThat(jwtService.extractUserId(decoded)).isEqualTo(user.id());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("OWNER");
    }

    @Test
    void validateAndDecode_invalidToken_throws() {
        assertThatThrownBy(() -> jwtService.validateAndDecode("not.a.jwt"))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void validateAndDecode_tamperedToken_throws() {
        User user = testUser(User.Role.MEMBER);
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.validateAndDecode(tampered))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void generateToken_differentSecret_notAccepted() {
        JwtService other = new JwtService("different-secret-that-is-also-32-chars-long!!", 24);
        String token = jwtService.generateToken(testUser(User.Role.MEMBER));

        assertThatThrownBy(() -> other.validateAndDecode(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    private User testUser(User.Role role) {
        return new User(UUID.randomUUID(), UUID.randomUUID(),
                "alice@example.com", "hashed", role, OffsetDateTime.now());
    }
}
