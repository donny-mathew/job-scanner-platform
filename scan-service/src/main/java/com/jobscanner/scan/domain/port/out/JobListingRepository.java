package com.jobscanner.scan.domain.port.out;

import com.jobscanner.scan.domain.model.JobListing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobListingRepository {
    JobListing save(JobListing listing);
    Optional<JobListing> findByExternalId(String externalId);
    Optional<JobListing> findById(UUID id);
    List<JobListing> findAllForCurrentTenant();
    boolean existsByExternalId(String externalId);

    /** Direct lookup by id without tenant scoping — for internal service-to-service calls. */
    Optional<JobListing> findByIdInternal(UUID id);
}
