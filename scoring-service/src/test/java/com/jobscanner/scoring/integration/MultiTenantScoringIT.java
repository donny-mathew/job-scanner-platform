package com.jobscanner.scoring.integration;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.in.ScoreJobUseCase;
import com.jobscanner.scoring.domain.port.in.ScoringProfileUseCase;
import com.jobscanner.scoring.domain.port.out.JobListingClient;
import com.jobscanner.scoring.domain.port.out.JobScoreRepository;
import com.jobscanner.scoring.domain.value.JobListingDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Proves multi-tenant isolation in the scoring pipeline:
 * - Each tenant has its own scoring profile
 * - JobScores are scoped to the correct tenant
 * - One tenant cannot see another tenant's scores
 */
@SpringBootTest
@Testcontainers
class MultiTenantScoringIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("scoringdb").withUsername("test").withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("app.jwt.secret", () -> "integration-test-secret-min-32-chars!!");
        r.add("app.scorer", () -> "mock");
        r.add("app.scan-service.base-url", () -> "http://localhost:9999"); // mocked below
    }

    @Autowired ScoringProfileUseCase profileUseCase;
    @Autowired ScoreJobUseCase scoreJobUseCase;
    @Autowired JobScoreRepository jobScoreRepository;

    // Mock the REST client so we don't need scan-service running
    @MockBean JobListingClient jobListingClient;

    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void eachTenantScoresWithOwnProfile_andCannotSeeOtherTenantScores() {
        // Setup Tenant A profile
        TenantContext.set(tenantA);
        ScoringProfile profileA = profileUseCase.upsert("Java/Spring Boot lead seeking AU sponsorship");
        assertThat(profileA.tenantId()).isEqualTo(tenantA);
        TenantContext.clear();

        // Setup Tenant B profile
        TenantContext.set(tenantB);
        ScoringProfile profileB = profileUseCase.upsert("Python/Django engineer, remote first");
        assertThat(profileB.tenantId()).isEqualTo(tenantB);
        TenantContext.clear();

        // Both have different profiles
        assertThat(profileA.profileText()).isNotEqualTo(profileB.profileText());

        UUID jobIdForA = UUID.randomUUID();
        UUID jobIdForB = UUID.randomUUID();

        when(jobListingClient.fetchJobListing(jobIdForA)).thenReturn(Optional.of(
                new JobListingDto(jobIdForA, tenantA, "Senior Java Engineer",
                        "Acme AU", "Sydney", "https://example.com/a", "{}")));
        when(jobListingClient.fetchJobListing(jobIdForB)).thenReturn(Optional.of(
                new JobListingDto(jobIdForB, tenantB, "Lead Python Developer",
                        "StartupB", "Melbourne", "https://example.com/b", "{}")));

        // Score for Tenant A
        JobScore scoreA = scoreJobUseCase.scoreJob(tenantA, jobIdForA);
        assertThat(scoreA.tenantId()).isEqualTo(tenantA);
        assertThat(scoreA.jobListingId()).isEqualTo(jobIdForA);

        // Score for Tenant B
        JobScore scoreB = scoreJobUseCase.scoreJob(tenantB, jobIdForB);
        assertThat(scoreB.tenantId()).isEqualTo(tenantB);
        assertThat(scoreB.jobListingId()).isEqualTo(jobIdForB);

        // Tenant A scope: sees only Tenant A's score
        TenantContext.set(tenantA);
        List<JobScore> scoresForA = jobScoreRepository.findAllForCurrentTenant();
        assertThat(scoresForA).hasSize(1);
        assertThat(scoresForA.get(0).tenantId()).isEqualTo(tenantA);
        assertThat(scoresForA).noneMatch(s -> s.tenantId().equals(tenantB));
        TenantContext.clear();

        // Tenant B scope: sees only Tenant B's score
        TenantContext.set(tenantB);
        List<JobScore> scoresForB = jobScoreRepository.findAllForCurrentTenant();
        assertThat(scoresForB).hasSize(1);
        assertThat(scoresForB.get(0).tenantId()).isEqualTo(tenantB);
        assertThat(scoresForB).noneMatch(s -> s.tenantId().equals(tenantA));
    }

    @Test
    void scoringProfile_tenantA_doesNotSeeTenantsB_profile() {
        TenantContext.set(tenantA);
        profileUseCase.upsert("Profile for tenant A only");
        TenantContext.clear();

        // Tenant B has no profile — reading it should throw
        TenantContext.set(tenantB);
        org.junit.jupiter.api.Assertions.assertThrows(
                com.jobscanner.scoring.domain.exception.ScoringProfileNotFoundException.class,
                () -> profileUseCase.getForCurrentTenant());
    }
}
