package com.cars24.redis.ratelimiter.config;

import lombok.Data;

@Data
public class RateLimiterConfig {
    private String redisHost;
    private int redisPort;
    private int maxRequests;
    private int timeWindowSeconds;

    public RateLimiterConfig(String redisHost, int redisPort, int maxRequests, int timeWindowSeconds) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.maxRequests = maxRequests;
        this.timeWindowSeconds = timeWindowSeconds;
    }

}
