package com.jobscanner.scoring.application.service;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.exception.JobListingNotFoundException;
import com.jobscanner.scoring.domain.exception.ScoringProfileNotFoundException;
import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.in.ScoreJobUseCase;
import com.jobscanner.scoring.domain.port.out.*;
import com.jobscanner.scoring.domain.port.out.JobScoredEventPort;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ScoreJobService implements ScoreJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScoreJobService.class);

    private final ScoringProfileRepository profileRepository;
    private final JobScoreRepository scoreRepository;
    private final JobListingClient jobListingClient;
    private final JobScorer scorer;
    private final ScoringCachePort cache;
    private final JobScoredEventPort eventPort;

    public ScoreJobService(ScoringProfileRepository profileRepository,
                           JobScoreRepository scoreRepository,
                           JobListingClient jobListingClient,
                           JobScorer scorer,
                           ScoringCachePort cache,
                           JobScoredEventPort eventPort) {
        this.profileRepository = profileRepository;
        this.scoreRepository = scoreRepository;
        this.jobListingClient = jobListingClient;
        this.scorer = scorer;
        this.cache = cache;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional
    public JobScore scoreJob(UUID tenantId, UUID jobListingId) {
        TenantContext.set(tenantId);
        try {
            // Idempotency: skip if already scored
            return scoreRepository.findByJobListingId(jobListingId)
                    .orElseGet(() -> computeAndPersist(tenantId, jobListingId));
        } finally {
            TenantContext.clear();
        }
    }

    private JobScore computeAndPersist(UUID tenantId, UUID jobListingId) {
        ScoringProfile profile = profileRepository.findByTenantId(tenantId)
                .orElseThrow(ScoringProfileNotFoundException::new);

        JobListingDto listing = jobListingClient.fetchJobListing(jobListingId)
                .orElseThrow(() -> new JobListingNotFoundException(jobListingId));

        String jobContent = listing.title() + " " + listing.company() + " " + listing.rawPayload();
        String cacheKey = cache.buildKey(profile.profileText(), jobContent);

        ScoreResult result = cache.get(cacheKey).orElseGet(() -> {
            log.debug("[tenant={}] Cache miss for job {}. Calling scorer.", tenantId, jobListingId);
            ScoreResult fresh = scorer.score(profile, listing);
            cache.put(cacheKey, fresh);
            return fresh;
        });

        JobScore score = new JobScore(UUID.randomUUID(), tenantId, jobListingId,
                result.score(), result.reasoning(), result.model(), OffsetDateTime.now());
        JobScore saved = scoreRepository.save(score);
        eventPort.publish(saved, listing);
        return saved;
    }
}
