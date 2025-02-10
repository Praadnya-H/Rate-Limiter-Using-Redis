package com.cars24.redis.ratelimiter;

import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String TEST_USER_ID = "test-user";
    private static JedisPool jedisPool;
    private RateLimiter rateLimiter;

    @BeforeAll
    static void setUp() {
        jedisPool = new JedisPool(REDIS_HOST, REDIS_PORT);
    }

    @BeforeEach
    void init() {
        // Clear Redis before each test
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    @Test
    void testBasicRateLimiting() {
        // Configure rate limiter: 5 requests per 1 second
        rateLimiter = new RateLimiter(REDIS_HOST, REDIS_PORT, 5, 1);

        // First 5 requests should succeed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(TEST_USER_ID),
                    "Request " + (i + 1) + " should be allowed");
        }

        // 6th request should fail
        assertFalse(rateLimiter.tryAcquire(TEST_USER_ID),
                "Request should be blocked after limit exceeded");
    }

    @Test
    void testSlidingWindow() throws InterruptedException {
        // Configure rate limiter: 2 requests per 2 seconds
        rateLimiter = new RateLimiter(REDIS_HOST, REDIS_PORT, 2, 2);

        // First 2 requests succeed
        assertTrue(rateLimiter.tryAcquire(TEST_USER_ID));
        assertTrue(rateLimiter.tryAcquire(TEST_USER_ID));

        // Third request fails
        assertFalse(rateLimiter.tryAcquire(TEST_USER_ID));

        // Wait for 2 seconds
        Thread.sleep(2000);

        // Should be able to make requests again
        assertTrue(rateLimiter.tryAcquire(TEST_USER_ID));
        assertTrue(rateLimiter.tryAcquire(TEST_USER_ID));
    }

    @Test
    void testMultipleUsers() {
        // Configure rate limiter: 3 requests per second
        rateLimiter = new RateLimiter(REDIS_HOST, REDIS_PORT, 3, 1);
        String user1 = "user1";
        String user2 = "user2";

        // User 1's requests
        assertTrue(rateLimiter.tryAcquire(user1));
        assertTrue(rateLimiter.tryAcquire(user1));
        assertTrue(rateLimiter.tryAcquire(user1));
        assertFalse(rateLimiter.tryAcquire(user1));

        // User 2 should still be able to make requests
        assertTrue(rateLimiter.tryAcquire(user2));
        assertTrue(rateLimiter.tryAcquire(user2));
        assertTrue(rateLimiter.tryAcquire(user2));
        assertFalse(rateLimiter.tryAcquire(user2));
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        // Configure rate limiter: 5 requests per second
        rateLimiter = new RateLimiter(REDIS_HOST, REDIS_PORT, 5, 1);
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        int[] successCount = {0};

        // Create 10 concurrent threads
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                if (rateLimiter.tryAcquire(TEST_USER_ID)) {
                    synchronized (successCount) {
                        successCount[0]++;
                    }
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(5, successCount[0],
                "Only 5 requests should succeed regardless of concurrent access");
    }

    @AfterEach
    void cleanup() {
        // Clean up Redis after each test
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    @AfterAll
    static void tearDown() {
        jedisPool.close();
    }
}
