package com.jobscanner.scoring.adapter.in.web;

import com.jobscanner.scoring.adapter.out.security.TenantContext;
import com.jobscanner.scoring.domain.model.JobScore;
import com.jobscanner.scoring.domain.port.in.ScoreJobUseCase;
import com.jobscanner.scoring.domain.port.out.JobScoreRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-scores")
public class JobScoreController {

    private final JobScoreRepository scoreRepository;
    private final ScoreJobUseCase scoreJobUseCase;

    public JobScoreController(JobScoreRepository scoreRepository, ScoreJobUseCase scoreJobUseCase) {
        this.scoreRepository = scoreRepository;
        this.scoreJobUseCase = scoreJobUseCase;
    }

    @GetMapping
    public List<JobScoreResponse> list() {
        return scoreRepository.findAllForCurrentTenant().stream()
                .map(JobScoreResponse::from).toList();
    }

    @GetMapping("/{jobListingId}")
    public JobScoreResponse getForJob(@PathVariable UUID jobListingId) {
        return JobScoreResponse.from(
                scoreJobUseCase.scoreJob(TenantContext.requireTenantId(), jobListingId));
    }

    record JobScoreResponse(UUID id, UUID tenantId, UUID jobListingId, int score,
                            String reasoning, String model, OffsetDateTime scoredAt) {
        static JobScoreResponse from(JobScore s) {
            return new JobScoreResponse(s.id(), s.tenantId(), s.jobListingId(),
                    s.score(), s.reasoning(), s.model(), s.scoredAt());
        }
    }
}
