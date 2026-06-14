package com.jobscanner.scoring.adapter.in.messaging;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.port.in.ScoreJobUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobDiscoveredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobDiscoveredEventConsumer.class);

    private final ScoreJobUseCase scoreJobUseCase;

    public JobDiscoveredEventConsumer(ScoreJobUseCase scoreJobUseCase) {
        this.scoreJobUseCase = scoreJobUseCase;
    }

    @KafkaListener(topics = "job-discovered", groupId = "scoring-service")
    public void consume(JobDiscoveredEventDto event) {
        if (event.tenantId() == null || event.jobListingId() == null) {
            log.warn("Received malformed job-discovered event, skipping");
            return;
        }
        log.info("[tenant={}] Received job-discovered event for listing {}",
                event.tenantId(), event.jobListingId());
        try {
            scoreJobUseCase.scoreJob(event.tenantId(), event.jobListingId());
        } catch (Exception e) {
            log.error("[tenant={}] Failed to score job listing {}: {}",
                    event.tenantId(), event.jobListingId(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
