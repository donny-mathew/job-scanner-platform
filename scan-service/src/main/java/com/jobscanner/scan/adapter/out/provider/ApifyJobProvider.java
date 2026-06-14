package com.jobscanner.scan.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobscanner.scan.domain.port.out.JobDataProvider;
import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApifyJobProvider implements JobDataProvider {

    private static final Logger log = LoggerFactory.getLogger(ApifyJobProvider.class);
    private static final String APIFY_BASE = "https://api.apify.com/v2";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final WebClient webClient;
    private final String token;
    private final String actorId;

    public ApifyJobProvider(
            @Value("${app.apify.token}") String token,
            @Value("${app.apify.actor-id}") String actorId) {
        this.token = token;
        this.actorId = actorId;
        this.webClient = WebClient.builder()
                .baseUrl(APIFY_BASE)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    @Override
    public List<RawJob> fetchJobs(SearchCriteria criteria) {
        // Trigger actor run
        Map<String, Object> input = Map.of(
                "keywords", criteria.keywords(),
                "location", criteria.location() != null ? criteria.location() : "",
                "aiVisaSponsorshipOnly", criteria.aiVisaSponsorshipOnly()
        );

        String runId = triggerActorRun(input);
        if (runId == null) return List.of();

        // Poll for completion and fetch dataset
        return fetchDataset(runId);
    }

    private String triggerActorRun(Map<String, Object> input) {
        try {
            JsonNode response = webClient.post()
                    .uri("/acts/{actorId}/runs", actorId)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(TIMEOUT);

            if (response != null && response.has("data")) {
                return response.get("data").get("id").asText();
            }
        } catch (Exception e) {
            log.error("Failed to trigger Apify actor run: {}", e.getMessage());
        }
        return null;
    }

    private List<RawJob> fetchDataset(String runId) {
        try {
            // Wait for run to finish (simplified polling)
            Thread.sleep(10_000);

            JsonNode dataset = webClient.get()
                    .uri("/actor-runs/{runId}/dataset/items", runId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(TIMEOUT);

            if (dataset == null || !dataset.isArray()) return List.of();

            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : dataset) {
                jobs.add(new RawJob(
                        item.path("id").asText(UUID.randomUUID().toString()),
                        item.path("title").asText("Unknown Title"),
                        item.path("company").asText("Unknown Company"),
                        item.path("location").asText(""),
                        item.path("url").asText(""),
                        item.toString(),
                        "apify"
                ));
            }
            return jobs;
        } catch (Exception e) {
            log.error("Failed to fetch Apify dataset for run {}: {}", runId, e.getMessage());
            return List.of();
        }
    }
}
