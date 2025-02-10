package com.cars24.redis.ratelimiter.controller;

import com.cars24.redis.ratelimiter.exception.RateLimitException;
import com.cars24.redis.ratelimiter.RateLimiter;
import com.cars24.redis.ratelimiter.config.RateLimiterConfig;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final RateLimiter rateLimiter;

    @Autowired
    public ApiController(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiter = new RateLimiter(rateLimiterConfig);
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getResource(@RequestHeader("X-User-Id") String userId) {
        if (!rateLimiter.tryAcquire(userId)) {
            throw new RateLimitException("Rate limit exceeded");
        }
        return ResponseEntity.ok("Resource accessed successfully");
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<String> handleRateLimitException(RateLimitException ex) {
        return ResponseEntity.status(429).body("Too Many Requests: " + ex.getMessage());
    }
}
