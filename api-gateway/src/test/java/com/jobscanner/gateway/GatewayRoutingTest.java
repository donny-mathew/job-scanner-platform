package com.jobscanner.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.jobscanner.gateway.support.AlwaysAllowRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    static MockWebServer mockSearchService = new MockWebServer();
    static MockWebServer mockAuthService = new MockWebServer();

    static final String SECRET = "test-secret-at-least-32-chars-long!!";

    @BeforeAll
    static void startMocks() throws IOException {
        mockSearchService.start();
        mockAuthService.start();
    }

    @AfterAll
    static void stopMocks() throws IOException {
        mockSearchService.shutdown();
        mockAuthService.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.jwt.secret", () -> SECRET);
        r.add("app.routes.auth-service", () -> "http://localhost:" + mockAuthService.getPort());
        r.add("app.routes.scan-service", () -> "http://localhost:9991");
        r.add("app.routes.scoring-service", () -> "http://localhost:9992");
        r.add("app.routes.search-service", () -> "http://localhost:" + mockSearchService.getPort());
        r.add("spring.data.redis.host", () -> "localhost");
        r.add("spring.data.redis.port", () -> "6370");
    }

    @TestConfiguration
    static class RateLimiterStubConfig {
        @Bean
        @Primary
        RateLimiter<?> alwaysAllowRateLimiter() {
            return new AlwaysAllowRateLimiter();
        }
    }

    @LocalServerPort int port;
    @Autowired WebTestClient webTestClient;

    private String buildJwt() { return buildJwtForTenant(UUID.randomUUID()); }

    private String buildJwtForTenant(UUID tenantId) {
        return JWT.create()
                .withIssuer("job-scanner-auth")
                .withSubject(UUID.randomUUID().toString())
                .withClaim("tenant_id", tenantId.toString())
                .withClaim("role", "OWNER")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    @Test
    void unauthenticatedRequest_toProtectedRoute_returns401() {
        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/jobs/search")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedRequest_isForwardedToDownstream() {
        mockSearchService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/jobs/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void invalidJwt_returns401() {
        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/jobs/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedRequest_downstreamReceivesXTenantIdHeader() throws InterruptedException {
        mockSearchService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        UUID tenantId = UUID.randomUUID();
        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/jobs/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwtForTenant(tenantId))
                .exchange();

        okhttp3.mockwebserver.RecordedRequest recorded = mockSearchService.takeRequest();
        Assertions.assertEquals(tenantId.toString(), recorded.getHeader("X-Tenant-Id"));
    }
}
