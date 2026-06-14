package com.jobscanner.scan.domain.port.in;

import java.util.UUID;

public interface ScanUseCase {
    /** Runs all enabled search configs for the given tenant. Returns count of new jobs found. */
    int runScan(UUID tenantId);
}
