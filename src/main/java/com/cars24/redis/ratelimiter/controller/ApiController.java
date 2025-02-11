package com.cars24.redis.ratelimiter.controller;

import com.cars24.redis.ratelimiter.exception.RateLimitException;
import com.cars24.redis.ratelimiter.RateLimiter;
import com.cars24.redis.ratelimiter.config.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {
    private final RateLimiter rateLimiter;

    @Autowired
    public ApiController(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiter = new RateLimiter(rateLimiterConfig);
        log.info("ApiController initialized with rate limiter settings: maxRequests={}, timeWindowSeconds={}",
                rateLimiterConfig.getMaxRequests(), rateLimiterConfig.getTimeWindowSeconds());
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getResource(@RequestHeader("X-User-Id") String userId) {
        log.info("Received request for /resource from user: {}", userId);
        if (!rateLimiter.tryAcquire(userId)) {
            log.warn("Rate limit exceeded for user: {}", userId);
            throw new RateLimitException("Rate limit exceeded");
        }
        log.debug("Request allowed for user: {}", userId);
        return ResponseEntity.ok("Resource accessed successfully");
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<String> handleRateLimitException(RateLimitException ex) {
        log.error("Rate limit exception: {}", ex.getMessage());
        return ResponseEntity.status(429).body("Too Many Requests: " + ex.getMessage());
    }
}
