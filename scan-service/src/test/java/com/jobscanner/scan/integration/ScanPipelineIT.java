package com.jobscanner.scan.integration;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.in.ScanUseCase;
import com.jobscanner.scan.domain.port.in.SearchConfigUseCase;
import com.jobscanner.scan.domain.port.out.JobListingRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full pipeline: create config → run scan (MockJobProvider) → assert jobs persisted
 * and job-discovered events published to Kafka. Also proves dedup (second run = 0 new jobs).
 */
@SpringBootTest
@Testcontainers
class ScanPipelineIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("scandb").withUsername("test").withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("app.jwt.secret", () -> "integration-test-secret-min-32-chars!!");
        r.add("app.job-provider", () -> "mock");
        r.add("app.scan.scheduler-enabled", () -> "false");
    }

    @Autowired SearchConfigUseCase searchConfigUseCase;
    @Autowired ScanUseCase scanUseCase;
    @Autowired JobListingRepository jobListingRepository;

    UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void scanPipeline_newJobsPersistedAndEventsPublished() {
        // Create a search config
        TenantContext.set(tenantId);
        SearchConfig config = searchConfigUseCase.create(
                new SearchConfigUseCase.CreateCommand(
                        List.of("java", "spring boot"), "Australia", "{}", null, true));
        assertThat(config.id()).isNotNull();

        // Run scan — MockJobProvider returns 3 canned jobs
        int newJobs = scanUseCase.runScan(tenantId);
        assertThat(newJobs).isEqualTo(3);

        // Assert jobs persisted in tenant scope
        TenantContext.set(tenantId);
        List<JobListing> listings = jobListingRepository.findAllForCurrentTenant();
        assertThat(listings).hasSize(3);
        assertThat(listings).allMatch(l -> l.tenantId().equals(tenantId));
        assertThat(listings).allMatch(l -> l.source().equals("mock"));

        // Assert Kafka events published
        int eventCount = consumeKafkaEvents("job-discovered", 3, kafka.getBootstrapServers());
        assertThat(eventCount).isEqualTo(3);
    }

    @Test
    void scanPipeline_secondRun_noDuplicates() {
        TenantContext.set(UUID.randomUUID()); // fresh tenant
        UUID freshTenantId = TenantContext.get();

        searchConfigUseCase.create(new SearchConfigUseCase.CreateCommand(
                List.of("kotlin"), "Remote", "{}", null, true));

        scanUseCase.runScan(freshTenantId);
        int secondRunCount = scanUseCase.runScan(freshTenantId);

        assertThat(secondRunCount).isZero();

        TenantContext.set(freshTenantId);
        assertThat(jobListingRepository.findAllForCurrentTenant()).hasSize(3);
    }

    private int consumeKafkaEvents(String topic, int expected, String bootstrapServers) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ))) {
            consumer.subscribe(List.of(topic));
            int received = 0;
            long deadline = System.currentTimeMillis() + 15_000;
            while (received < expected && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                received += records.count();
            }
            return received;
        }
    }
}
