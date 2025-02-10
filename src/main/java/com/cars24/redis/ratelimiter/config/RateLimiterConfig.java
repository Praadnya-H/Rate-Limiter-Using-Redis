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

//    // Getters and setters
//    public String getRedisHost() { return redisHost; }
//    public int getRedisPort() { return redisPort; }
//    public int getMaxRequests() { return maxRequests; }
//    public int getTimeWindowSeconds() { return timeWindowSeconds; }
}
