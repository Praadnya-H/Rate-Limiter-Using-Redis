package com.cars24.redis.ratelimiter;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import com.cars24.redis.ratelimiter.config.RateLimiterConfig;
import com.cars24.redis.ratelimiter.exception.RedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@Slf4j
public class RateLimiter {
    private final JedisPool jedisPool;
    private final int maxRequests;
    private final int timeWindowSeconds;

    public RateLimiter(RateLimiterConfig config) {
        log.info("Initializing RateLimiter with maxRequests={} and timeWindowSeconds={}",
                config.getMaxRequests(), config.getTimeWindowSeconds());
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        try {
            this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000);
            try (Jedis jedis = jedisPool.getResource()) {
                log.info("Successfully connected to Redis at {}:{}", config.getRedisHost(), config.getRedisPort());
                jedis.ping();
            }
        } catch (JedisException e) {
            log.error("Failed to establish Redis connection: {}", e.getMessage(), e);
            throw new RedisConnectionException("Failed to establish Redis connection: " + e.getMessage(), e);
        }

        this.maxRequests = config.getMaxRequests();
        this.timeWindowSeconds = config.getTimeWindowSeconds();
    }

    public boolean tryAcquire(String userId) {
        log.debug("Processing request for user: {}", userId);
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "rate_limiter:" + userId;
            long currentTime = System.currentTimeMillis();

            Transaction transaction = jedis.multi();
            transaction.zremrangeByScore(key, 0, currentTime - timeWindowSeconds * 1000);
            Response<Long> countResponse = transaction.zcard(key);
            transaction.zadd(key, currentTime, String.valueOf(currentTime));
            transaction.expire(key, timeWindowSeconds);
            transaction.exec();
            boolean isAllowed = countResponse.get() < maxRequests;
            if (isAllowed) {
                log.info("Request ALLOWED for user {} (requests in window: {}/{})", userId, countResponse.get() + 1, maxRequests);
            } else {
                log.warn("Request BLOCKED for user {} (requests in window: {}/{})", userId, countResponse.get(), maxRequests);
            }
            return isAllowed;
        } catch (JedisException e) {
            log.error("Redis communication error for user {}: {}", userId, e.getMessage(), e);
            throw new RedisConnectionException("Failed to communicate with Redis: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
        if (jedisPool != null) {
            log.info("Shutting down Redis connection pool...");
            jedisPool.close();
            log.info("Redis connection pool closed successfully.");
        }
    }
}
