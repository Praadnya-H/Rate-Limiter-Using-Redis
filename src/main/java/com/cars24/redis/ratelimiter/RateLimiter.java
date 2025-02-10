package com.cars24.redis.ratelimiter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

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
        this.jedisPool = new JedisPool(poolConfig, redisHost, port);
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
        }
    }

    public void shutdown() {
        jedisPool.close();
    }
}
