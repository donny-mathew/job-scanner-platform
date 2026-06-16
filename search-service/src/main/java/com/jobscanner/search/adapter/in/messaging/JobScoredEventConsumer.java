package com.jobscanner.search.adapter.in.messaging;

import com.jobscanner.search.adapter.out.security.TenantContext;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.in.IndexJobUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class JobScoredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobScoredEventConsumer.class);

    private final IndexJobUseCase indexJobUseCase;

    public JobScoredEventConsumer(IndexJobUseCase indexJobUseCase) {
        this.indexJobUseCase = indexJobUseCase;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "job-scored", groupId = "search-service")
    public void consume(JobScoredEventDto event) {
        if (event.tenantId() == null || event.jobListingId() == null) {
            log.warn("Received malformed job-scored event, skipping");
            return;
        }
        log.info("[tenant={}] Indexing scored job {}", event.tenantId(), event.jobListingId());
        TenantContext.set(event.tenantId());
        try {
            JobSearchDocument document = new JobSearchDocument(
                    event.jobListingId(),
                    event.tenantId(),
                    event.title(),
                    event.company(),
                    event.location(),
                    event.url(),
                    event.source(),
                    event.discoveredAt(),
                    event.score(),
                    event.reasoning(),
                    event.model(),
                    event.scoredAt()
            );
            indexJobUseCase.index(document);
        } finally {
            TenantContext.clear();
        }
    }

    @DltHandler
    public void handleDlt(JobScoredEventDto event) {
        log.error("[tenant={}] Indexing permanently failed after all retries — message parked in DLT. jobListingId={}",
                event.tenantId(), event.jobListingId());
    }
}
