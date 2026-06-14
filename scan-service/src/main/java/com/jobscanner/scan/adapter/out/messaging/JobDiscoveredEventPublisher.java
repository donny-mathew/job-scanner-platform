package com.jobscanner.scan.adapter.out.messaging;

import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.port.out.JobDiscoveredEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobDiscoveredEventPublisher implements JobDiscoveredEventPort {

    private static final Logger log = LoggerFactory.getLogger(JobDiscoveredEventPublisher.class);
    static final String TOPIC = "job-discovered";

    private final KafkaTemplate<String, JobDiscoveredEvent> kafkaTemplate;

    public JobDiscoveredEventPublisher(KafkaTemplate<String, JobDiscoveredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(JobListing listing) {
        JobDiscoveredEvent event = new JobDiscoveredEvent(
                UUID.randomUUID(),
                listing.tenantId(),
                listing.id(),
                listing.externalId(),
                listing.title(),
                listing.source(),
                listing.discoveredAt()
        );
        kafkaTemplate.send(TOPIC, listing.tenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish job-discovered event for listing {}: {}",
                                listing.id(), ex.getMessage());
                    } else {
                        log.debug("Published job-discovered event for listing {}", listing.id());
                    }
                });
    }
}
