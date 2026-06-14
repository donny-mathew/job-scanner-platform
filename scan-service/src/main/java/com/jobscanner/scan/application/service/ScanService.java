package com.jobscanner.scan.application.service;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.in.ScanUseCase;
import com.jobscanner.scan.domain.port.out.JobDataProvider;
import com.jobscanner.scan.domain.port.out.JobListingRepository;
import com.jobscanner.scan.domain.port.out.JobDiscoveredEventPort;
import com.jobscanner.scan.domain.port.out.SearchConfigRepository;
import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScanService implements ScanUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final SearchConfigRepository searchConfigRepository;
    private final JobListingRepository jobListingRepository;
    private final JobDataProvider jobDataProvider;
    private final JobDiscoveredEventPort eventPort;

    public ScanService(SearchConfigRepository searchConfigRepository,
                       JobListingRepository jobListingRepository,
                       JobDataProvider jobDataProvider,
                       JobDiscoveredEventPort eventPort) {
        this.searchConfigRepository = searchConfigRepository;
        this.jobListingRepository = jobListingRepository;
        this.jobDataProvider = jobDataProvider;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional
    public int runScan(UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            List<SearchConfig> configs = searchConfigRepository.findAllForCurrentTenant()
                    .stream()
                    .filter(SearchConfig::enabled)
                    .toList();

            log.info("[tenant={}] Running scan for {} config(s)", tenantId, configs.size());

            int newJobCount = 0;
            for (SearchConfig config : configs) {
                newJobCount += processConfig(tenantId, config);
            }

            log.info("[tenant={}] Scan complete. {} new job(s) discovered.", tenantId, newJobCount);
            return newJobCount;
        } finally {
            TenantContext.clear();
        }
    }

    private int processConfig(UUID tenantId, SearchConfig config) {
        SearchCriteria criteria = new SearchCriteria(config.keywords(), config.location(), false);
        List<RawJob> rawJobs = jobDataProvider.fetchJobs(criteria);

        int newCount = 0;
        for (RawJob raw : rawJobs) {
            if (jobListingRepository.existsByExternalId(raw.externalId())) {
                log.debug("[tenant={}] Job already seen: {}", tenantId, raw.externalId());
                continue;
            }
            JobListing listing = new JobListing(
                    UUID.randomUUID(), tenantId, raw.externalId(),
                    raw.title(), raw.company(), raw.location(),
                    raw.url(), raw.rawPayload(), raw.source(),
                    OffsetDateTime.now()
            );
            JobListing saved = jobListingRepository.save(listing);
            eventPort.publish(saved);
            newCount++;
        }
        return newCount;
    }
}
