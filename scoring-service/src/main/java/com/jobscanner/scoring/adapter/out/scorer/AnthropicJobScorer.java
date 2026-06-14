package com.jobscanner.scoring.adapter.out.scorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.out.JobScorer;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class AnthropicJobScorer implements JobScorer {

    private static final Logger log = LoggerFactory.getLogger(AnthropicJobScorer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicJobScorer(
            @Value("${app.anthropic.api-key}") String apiKey,
            @Value("${app.anthropic.model}") String model,
            @Value("${app.anthropic.base-url}") String baseUrl) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ScoreResult score(ScoringProfile profile, JobListingDto listing) {
        String prompt = buildPrompt(profile, listing);

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 256,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            JsonNode response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(TIMEOUT);

            String text = response.at("/content/0/text").asText("");
            return parseResponse(text);
        } catch (Exception e) {
            log.error("Anthropic API call failed for job {}: {}", listing.id(), e.getMessage());
            return new ScoreResult(0, "Scoring failed: " + e.getMessage(), model);
        }
    }

    private String buildPrompt(ScoringProfile profile, JobListingDto listing) {
        return """
                You are a job-fit scorer. Score how well this job matches the candidate profile.

                Candidate profile:
                %s

                Job listing:
                Title: %s
                Company: %s
                Location: %s
                Description: %s

                Respond with ONLY valid JSON in this exact format (no other text):
                {"score": <integer 0-100>, "reasoning": "<one sentence explanation>"}
                """.formatted(
                profile.profileText(),
                listing.title(),
                listing.company(),
                listing.location() != null ? listing.location() : "Not specified",
                truncate(listing.rawPayload(), 1000)
        );
    }

    private ScoreResult parseResponse(String text) {
        try {
            // Extract JSON — sometimes the model wraps it in markdown code blocks
            String json = text.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            int score = Math.max(0, Math.min(100, node.path("score").asInt(50)));
            String reasoning = node.path("reasoning").asText("No reasoning provided");
            return new ScoreResult(score, reasoning, model);
        } catch (Exception e) {
            log.warn("Failed to parse Anthropic response: {}. Raw: {}", e.getMessage(), text);
            return new ScoreResult(50, "Could not parse scoring response", model);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
