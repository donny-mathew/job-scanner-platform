package com.jobscanner.scan.domain.port.out;

import com.jobscanner.scan.domain.model.JobListing;

public interface JobDiscoveredEventPort {
    void publish(JobListing listing);
}
