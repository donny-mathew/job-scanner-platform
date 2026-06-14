package com.jobscanner.scoring.adapter.out.client;

import com.jobscanner.scoring.domain.port.out.JobListingClient;
import com.jobscanner.scoring.domain.value.JobListingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class JobListingRestClient implements JobListingClient {

    private static final Logger log = LoggerFactory.getLogger(JobListingRestClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public JobListingRestClient(@Value("${app.scan-service.base-url}") String scanServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(scanServiceUrl).build();
    }

    @Override
    public Optional<JobListingDto> fetchJobListing(UUID jobListingId) {
        try {
            JobListingDto dto = webClient.get()
                    .uri("/internal/job-listings/{id}", jobListingId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                        log.warn("Job listing {} not found in scan-service", jobListingId);
                        return null;
                    })
                    .bodyToMono(JobListingDto.class)
                    .block(TIMEOUT);
            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.error("Failed to fetch job listing {} from scan-service: {}", jobListingId, e.getMessage());
            return Optional.empty();
        }
    }
}
