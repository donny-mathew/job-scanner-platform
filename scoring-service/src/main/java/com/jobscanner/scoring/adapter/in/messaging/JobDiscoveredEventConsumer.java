package com.jobscanner.scoring.adapter.in.messaging;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.port.in.ScoreJobUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class JobDiscoveredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobDiscoveredEventConsumer.class);

    private final ScoreJobUseCase scoreJobUseCase;

    public JobDiscoveredEventConsumer(ScoreJobUseCase scoreJobUseCase) {
        this.scoreJobUseCase = scoreJobUseCase;
    }

    // 4 total attempts (1 original + 3 retries): 2s → 4s → 8s backoff.
    // On exhaustion, Spring Kafka routes the message to job-discovered-dlt automatically.
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "job-discovered", groupId = "scoring-service")
    public void consume(JobDiscoveredEventDto event) {
        if (event.tenantId() == null || event.jobListingId() == null) {
            log.warn("Received malformed job-discovered event, skipping");
            return;
        }
        log.info("[tenant={}] Scoring job listing {}", event.tenantId(), event.jobListingId());
        try {
            scoreJobUseCase.scoreJob(event.tenantId(), event.jobListingId());
        } finally {
            TenantContext.clear();
        }
    }

    @DltHandler
    public void handleDlt(JobDiscoveredEventDto event) {
        log.error("[tenant={}] Job scoring permanently failed after all retries — message parked in DLT. jobListingId={}",
                event.tenantId(), event.jobListingId());
    }
}
