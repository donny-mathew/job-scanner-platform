package com.jobscanner.scan.adapter.in.web;

import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.port.out.JobListingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal endpoint for service-to-service calls (no JWT required).
 * Only reachable within the Docker network — not exposed externally.
 */
@RestController
@RequestMapping("/internal/job-listings")
public class InternalJobListingController {

    private final JobListingRepository repository;

    public InternalJobListingController(JobListingRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobListingDto> getById(@PathVariable UUID id) {
        return repository.findByIdInternal(id)
                .map(l -> ResponseEntity.ok(JobListingDto.from(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    record JobListingDto(UUID id, UUID tenantId, String title, String company,
                         String location, String url, String rawPayload) {
        static JobListingDto from(JobListing l) {
            return new JobListingDto(l.id(), l.tenantId(), l.title(), l.company(),
                    l.location(), l.url(), l.rawPayload());
        }
    }
}
