package com.jobscanner.scan.adapter.out.security;

import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.in.ScanUseCase;
import com.jobscanner.scan.domain.port.out.SearchConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.scan.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScanScheduler.class);

    private final SearchConfigRepository searchConfigRepository;
    private final ScanUseCase scanUseCase;

    public ScanScheduler(SearchConfigRepository searchConfigRepository, ScanUseCase scanUseCase) {
        this.searchConfigRepository = searchConfigRepository;
        this.scanUseCase = scanUseCase;
    }

    @Scheduled(fixedRateString = "${app.scan.scheduler-interval-ms:60000}")
    public void runScheduledScans() {
        // Cross-tenant query to find all enabled configs
        List<SearchConfig> enabledConfigs = searchConfigRepository.findAllEnabled();
        Set<UUID> tenantIds = enabledConfigs.stream()
                .map(SearchConfig::tenantId)
                .collect(Collectors.toSet());

        log.info("Scheduled scan: {} tenant(s) with enabled configs", tenantIds.size());

        for (UUID tenantId : tenantIds) {
            try {
                scanUseCase.runScan(tenantId);
            } catch (Exception e) {
                log.error("Scheduled scan failed for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }
}
