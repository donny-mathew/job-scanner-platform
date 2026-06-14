package com.jobscanner.scoring.domain.exception;

import java.util.UUID;

public class JobListingNotFoundException extends RuntimeException {
    public JobListingNotFoundException(UUID id) {
        super("Job listing not found: " + id);
    }
}
