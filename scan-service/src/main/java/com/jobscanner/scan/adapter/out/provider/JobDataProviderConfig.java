package com.jobscanner.scan.adapter.out.provider;

import com.jobscanner.scan.domain.port.out.JobDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobDataProviderConfig {

    @Bean
    @ConditionalOnProperty(name = "app.job-provider", havingValue = "mock", matchIfMissing = true)
    public JobDataProvider mockJobProvider() {
        return new MockJobProvider();
    }

    @Bean
    @ConditionalOnProperty(name = "app.job-provider", havingValue = "apify")
    public JobDataProvider apifyJobProvider(
            @Value("${app.apify.token}") String token,
            @Value("${app.apify.actor-id}") String actorId) {
        return new ApifyJobProvider(token, actorId);
    }
}
