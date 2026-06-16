package com.jobscanner.scoring.adapter.out.messaging;

import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.port.out.JobScoredEventPort;
import com.jobscanner.scoring.domain.value.JobListingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobScoredEventPublisher implements JobScoredEventPort {

    static final String TOPIC = "job-scored";
    private static final Logger log = LoggerFactory.getLogger(JobScoredEventPublisher.class);

    private final KafkaTemplate<String, JobScoredEvent> kafkaTemplate;

    public JobScoredEventPublisher(KafkaTemplate<String, JobScoredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(JobScore score, JobListingDto listing) {
        JobScoredEvent event = new JobScoredEvent(
                UUID.randomUUID(),
                score.tenantId(),
                score.jobListingId(),
                listing.title(),
                listing.company(),
                listing.location(),
                listing.url(),
                listing.source(),
                listing.discoveredAt(),
                score.score(),
                score.reasoning(),
                score.model(),
                score.scoredAt()
        );
        kafkaTemplate.send(TOPIC, score.tenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[tenant={}] Failed to publish job-scored event for job {}: {}",
                                score.tenantId(), score.jobListingId(), ex.getMessage());
                    } else {
                        log.debug("[tenant={}] Published job-scored event for job {}",
                                score.tenantId(), score.jobListingId());
                    }
                });
    }
}
