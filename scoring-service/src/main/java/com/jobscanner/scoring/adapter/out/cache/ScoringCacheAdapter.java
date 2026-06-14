package com.jobscanner.scoring.adapter.out.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobscanner.scoring.domain.port.out.ScoringCachePort;
import com.jobscanner.scoring.domain.value.ScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class ScoringCacheAdapter implements ScoringCachePort {

    private static final Logger log = LoggerFactory.getLogger(ScoringCacheAdapter.class);
    private static final String KEY_PREFIX = "score:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public ScoringCacheAdapter(StringRedisTemplate redis,
                               @Value("${app.cache.score-ttl-seconds:604800}") long ttlSeconds) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public Optional<ScoreResult> get(String cacheKey) {
        try {
            String value = redis.opsForValue().get(KEY_PREFIX + cacheKey);
            if (value == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(value, ScoreResult.class));
        } catch (Exception e) {
            log.warn("Cache read failed for key {}: {}", cacheKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String cacheKey, ScoreResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redis.opsForValue().set(KEY_PREFIX + cacheKey, json, ttl);
        } catch (Exception e) {
            log.warn("Cache write failed for key {}: {}", cacheKey, e.getMessage());
        }
    }

    @Override
    public String buildKey(String profileText, String jobContent) {
        try {
            String combined = profileText + "|" + jobContent;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Fallback to a simple hash if SHA-256 unavailable (never in practice)
            return String.valueOf((profileText + jobContent).hashCode());
        }
    }
}
