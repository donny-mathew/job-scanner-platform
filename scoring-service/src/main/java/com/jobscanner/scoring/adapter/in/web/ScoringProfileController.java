package com.jobscanner.scoring.adapter.in.web;

import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.port.in.ScoringProfileUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scores/scoring-profile")
public class ScoringProfileController {

    private final ScoringProfileUseCase useCase;

    public ScoringProfileController(ScoringProfileUseCase useCase) { this.useCase = useCase; }

    @GetMapping
    public ScoringProfileResponse get() {
        return ScoringProfileResponse.from(useCase.getForCurrentTenant());
    }

    @PutMapping
    public ScoringProfileResponse upsert(@Valid @RequestBody UpsertRequest req) {
        return ScoringProfileResponse.from(useCase.upsert(req.profileText()));
    }

    record UpsertRequest(@NotBlank String profileText) {}

    record ScoringProfileResponse(UUID id, UUID tenantId, String profileText, OffsetDateTime updatedAt) {
        static ScoringProfileResponse from(ScoringProfile p) {
            return new ScoringProfileResponse(p.id(), p.tenantId(), p.profileText(), p.updatedAt());
        }
    }
}
