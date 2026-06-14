package com.jobscanner.scoring.adapter.out.scorer;

import com.jobscanner.scoring.domain.port.out.JobScorer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScorerConfig {

    @Bean
    @ConditionalOnProperty(name = "app.scorer", havingValue = "mock", matchIfMissing = true)
    public JobScorer mockJobScorer() {
        return new MockJobScorer();
    }

    @Bean
    @ConditionalOnProperty(name = "app.scorer", havingValue = "anthropic")
    public JobScorer anthropicJobScorer(
            @Value("${app.anthropic.api-key}") String apiKey,
            @Value("${app.anthropic.model}") String model,
            @Value("${app.anthropic.base-url}") String baseUrl) {
        return new AnthropicJobScorer(apiKey, model, baseUrl);
    }
}
