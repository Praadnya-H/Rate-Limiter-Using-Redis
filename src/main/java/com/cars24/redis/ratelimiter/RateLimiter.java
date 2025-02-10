package com.cars24.redis.ratelimiter;

import redis.clients.jedis.*;
import com.cars24.redis.ratelimiter.config.RateLimiterConfig;
import com.cars24.redis.ratelimiter.exception.RedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class RateLimiter {
    private final JedisPool jedisPool;
    private final int maxRequests;
    private final int timeWindowSeconds;

    public RateLimiter(RateLimiterConfig config) {
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
                jedis.ping();
            }
        } catch (JedisException e) {
            throw new RedisConnectionException("Failed to establish Redis connection: " + e.getMessage(), e);
        }

        this.maxRequests = config.getMaxRequests();
        this.timeWindowSeconds = config.getTimeWindowSeconds();
    }

    public boolean tryAcquire(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "rate_limiter:" + userId;
            long currentTime = System.currentTimeMillis();

            Transaction transaction = jedis.multi();
            transaction.zremrangeByScore(key, 0, currentTime - timeWindowSeconds * 1000);
            Response<Long> countResponse = transaction.zcard(key);
            transaction.zadd(key, currentTime, String.valueOf(currentTime));
            transaction.expire(key, timeWindowSeconds);
            transaction.exec();

            return countResponse.get() < maxRequests;
        } catch (JedisException e) {
            throw new RedisConnectionException("Failed to communicate with Redis: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
