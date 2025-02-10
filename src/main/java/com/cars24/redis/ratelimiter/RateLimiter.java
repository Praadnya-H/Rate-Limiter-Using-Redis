package com.cars24.redis.ratelimiter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import com.cars24.redis.ratelimiter.exception.RedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class RateLimiter {
    private final JedisPool jedisPool;
    private final int maxRequests;
    private final int timeWindowSeconds;

    public RateLimiter(String redisHost, int port, int maxRequests, int timeWindowSeconds) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        try {
            this.jedisPool = new JedisPool(poolConfig, redisHost, port, 2000); // 2 second timeout
            // Test connection during initialization
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
        } catch (JedisException e) {
            throw new RedisConnectionException("Failed to establish Redis connection: " + e.getMessage(), e);
        }

        this.maxRequests = maxRequests;
        this.timeWindowSeconds = timeWindowSeconds;
    }

    public boolean tryAcquire(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "rate_limiter:" + userId;
            long currentTime = System.currentTimeMillis();

            Transaction transaction = jedis.multi();

            // Remove counts older than our time window
            transaction.zremrangeByScore(key, 0, currentTime - timeWindowSeconds * 1000);

            // Count existing requests in the current window
            Response<Long> countResponse = transaction.zcard(key);

            // Add the current request timestamp
            transaction.zadd(key, currentTime, String.valueOf(currentTime));

            // Set key expiration
            transaction.expire(key, timeWindowSeconds);

            // Execute all commands atomically
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