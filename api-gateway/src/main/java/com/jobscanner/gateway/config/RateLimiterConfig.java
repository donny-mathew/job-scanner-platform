package com.jobscanner.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Value("${app.rate-limit.replenish-rate:20}")
    private int replenishRate;

    @Value("${app.rate-limit.burst-capacity:40}")
    private int burstCapacity;

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }
}
