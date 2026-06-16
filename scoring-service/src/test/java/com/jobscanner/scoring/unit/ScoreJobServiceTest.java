package com.jobscanner.scoring.unit;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.application.service.ScoreJobService;
import com.jobscanner.scoring.domain.exception.ScoringProfileNotFoundException;
import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.out.*;
import com.jobscanner.scoring.domain.port.out.JobScoredEventPort;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreJobServiceTest {

    @Mock ScoringProfileRepository profileRepository;
    @Mock JobScoreRepository scoreRepository;
    @Mock JobListingClient jobListingClient;
    @Mock JobScorer scorer;
    @Mock ScoringCachePort cache;
    @Mock JobScoredEventPort eventPort;

    ScoreJobService service;
    UUID tenantId = UUID.randomUUID();
    UUID jobListingId = UUID.randomUUID();
    ScoringProfile profile;
    JobListingDto listing;

    @BeforeEach
    void setUp() {
        service = new ScoreJobService(profileRepository, scoreRepository,
                jobListingClient, scorer, cache, eventPort);
        profile = new ScoringProfile(UUID.randomUUID(), tenantId,
                "Java/Spring Boot lead, 8 years experience, seeking AU visa sponsorship",
                OffsetDateTime.now());
        listing = new JobListingDto(jobListingId, tenantId, "Senior Java Engineer",
                "Acme Corp", "Sydney", "https://example.com", "{\"description\":\"Great role\"}",
                "mock", OffsetDateTime.now());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void scoreJob_cacheMiss_callsScorerAndCaches() {
        String cacheKey = "hash-abc";
        ScoreResult result = new ScoreResult(85, "Strong match", "mock");

        when(scoreRepository.findByJobListingId(jobListingId)).thenReturn(Optional.empty());
        when(profileRepository.findByTenantId(tenantId)).thenReturn(Optional.of(profile));
        when(jobListingClient.fetchJobListing(jobListingId)).thenReturn(Optional.of(listing));
        when(cache.buildKey(anyString(), anyString())).thenReturn(cacheKey);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(scorer.score(profile, listing)).thenReturn(result);
        when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JobScore score = service.scoreJob(tenantId, jobListingId);

        assertThat(score.score()).isEqualTo(85);
        assertThat(score.tenantId()).isEqualTo(tenantId);
        verify(scorer).score(profile, listing);
        verify(cache).put(cacheKey, result);
    }

    @Test
    void scoreJob_cacheHit_scorerNotCalled() {
        String cacheKey = "hash-xyz";
        ScoreResult cached = new ScoreResult(72, "Cached result", "mock");

        when(scoreRepository.findByJobListingId(jobListingId)).thenReturn(Optional.empty());
        when(profileRepository.findByTenantId(tenantId)).thenReturn(Optional.of(profile));
        when(jobListingClient.fetchJobListing(jobListingId)).thenReturn(Optional.of(listing));
        when(cache.buildKey(anyString(), anyString())).thenReturn(cacheKey);
        when(cache.get(cacheKey)).thenReturn(Optional.of(cached));
        when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JobScore score = service.scoreJob(tenantId, jobListingId);

        assertThat(score.score()).isEqualTo(72);
        verifyNoInteractions(scorer);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void scoreJob_alreadyScored_idempotent() {
        JobScore existing = new JobScore(UUID.randomUUID(), tenantId, jobListingId,
                90, "Already scored", "mock", OffsetDateTime.now());
        when(scoreRepository.findByJobListingId(jobListingId)).thenReturn(Optional.of(existing));

        JobScore result = service.scoreJob(tenantId, jobListingId);

        assertThat(result.score()).isEqualTo(90);
        verifyNoInteractions(profileRepository, jobListingClient, scorer, cache);
    }

    @Test
    void scoreJob_noProfile_throws() {
        when(scoreRepository.findByJobListingId(jobListingId)).thenReturn(Optional.empty());
        when(profileRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.scoreJob(tenantId, jobListingId))
                .isInstanceOf(ScoringProfileNotFoundException.class);
    }

    @Test
    void scoreJob_tenantContextClearedAfterCall() {
        when(scoreRepository.findByJobListingId(jobListingId)).thenReturn(Optional.empty());
        when(profileRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        try { service.scoreJob(tenantId, jobListingId); } catch (Exception ignored) {}

        assertThat(TenantContext.get()).isNull();
    }
}
