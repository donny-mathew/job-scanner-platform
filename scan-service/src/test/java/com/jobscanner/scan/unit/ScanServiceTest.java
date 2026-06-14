package com.jobscanner.scan.unit;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.application.service.ScanService;
import com.jobscanner.scan.domain.model.JobListing;
import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.out.JobDataProvider;
import com.jobscanner.scan.domain.port.out.JobDiscoveredEventPort;
import com.jobscanner.scan.domain.port.out.JobListingRepository;
import com.jobscanner.scan.domain.port.out.SearchConfigRepository;
import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock SearchConfigRepository searchConfigRepository;
    @Mock JobListingRepository jobListingRepository;
    @Mock JobDataProvider jobDataProvider;
    @Mock JobDiscoveredEventPort eventPort;

    ScanService scanService;
    UUID tenantId = UUID.randomUUID();
    SearchConfig enabledConfig;

    @BeforeEach
    void setUp() {
        scanService = new ScanService(searchConfigRepository, jobListingRepository,
                jobDataProvider, eventPort);
        enabledConfig = new SearchConfig(UUID.randomUUID(), tenantId,
                List.of("java"), "Australia", "{}", null, true, OffsetDateTime.now());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void runScan_newJob_persistedAndEventPublished() {
        RawJob rawJob = new RawJob("ext-1", "Senior Java Dev", "Acme", "Sydney",
                "https://example.com/1", "{}", "mock");

        when(searchConfigRepository.findAllForCurrentTenant()).thenReturn(List.of(enabledConfig));
        when(jobDataProvider.fetchJobs(any(SearchCriteria.class))).thenReturn(List.of(rawJob));
        when(jobListingRepository.existsByExternalId("ext-1")).thenReturn(false);
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = scanService.runScan(tenantId);

        assertThat(count).isEqualTo(1);
        verify(jobListingRepository).save(any(JobListing.class));
        verify(eventPort).publish(any(JobListing.class));
    }

    @Test
    void runScan_existingJob_notPersistedOrPublished() {
        RawJob rawJob = new RawJob("ext-already-seen", "Old Job", "Corp", "Melbourne",
                "https://example.com/2", "{}", "mock");

        when(searchConfigRepository.findAllForCurrentTenant()).thenReturn(List.of(enabledConfig));
        when(jobDataProvider.fetchJobs(any())).thenReturn(List.of(rawJob));
        when(jobListingRepository.existsByExternalId("ext-already-seen")).thenReturn(true);

        int count = scanService.runScan(tenantId);

        assertThat(count).isZero();
        verify(jobListingRepository, never()).save(any());
        verify(eventPort, never()).publish(any());
    }

    @Test
    void runScan_disabledConfig_skipped() {
        SearchConfig disabledConfig = new SearchConfig(UUID.randomUUID(), tenantId,
                List.of("java"), null, "{}", null, false, OffsetDateTime.now());

        when(searchConfigRepository.findAllForCurrentTenant()).thenReturn(List.of(disabledConfig));

        int count = scanService.runScan(tenantId);

        assertThat(count).isZero();
        verifyNoInteractions(jobDataProvider, jobListingRepository, eventPort);
    }

    @Test
    void runScan_savedJobHasCorrectTenantId() {
        RawJob rawJob = new RawJob("ext-2", "Lead Dev", "Startup", "Remote",
                "https://example.com/3", "{}", "mock");

        when(searchConfigRepository.findAllForCurrentTenant()).thenReturn(List.of(enabledConfig));
        when(jobDataProvider.fetchJobs(any())).thenReturn(List.of(rawJob));
        when(jobListingRepository.existsByExternalId("ext-2")).thenReturn(false);
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scanService.runScan(tenantId);

        ArgumentCaptor<JobListing> captor = ArgumentCaptor.forClass(JobListing.class);
        verify(jobListingRepository).save(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
    }

    @Test
    void runScan_tenantContextClearedAfterRun() {
        when(searchConfigRepository.findAllForCurrentTenant()).thenReturn(List.of());

        scanService.runScan(tenantId);

        assertThat(TenantContext.get()).isNull();
    }
}
