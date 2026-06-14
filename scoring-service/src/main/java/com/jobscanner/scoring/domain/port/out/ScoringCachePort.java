package com.jobscanner.scoring.domain.port.out;

import com.jobscanner.scoring.domain.value.ScoreResult;

import java.util.Optional;

public interface ScoringCachePort {
    Optional<ScoreResult> get(String cacheKey);
    void put(String cacheKey, ScoreResult result);
    String buildKey(String profileText, String jobContent);
}
