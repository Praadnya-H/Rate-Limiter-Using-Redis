package com.cars24.redis.ratelimiter.controller;

import com.cars24.redis.ratelimiter.exception.RateLimitException;
import com.cars24.redis.ratelimiter.RateLimiter;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final RateLimiter rateLimiter;

    public ApiController() {
        this.rateLimiter = new RateLimiter(
                "localhost",    // Redis host
                6379,          // Redis port
                5,           // Max requests
                60            // Time window in seconds
        );
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getResource(
            @RequestHeader("X-User-Id") String userId) {

        if (!rateLimiter.tryAcquire(userId)) {
            throw new RateLimitException("Rate limit exceeded");
        }

        // Process the request normally
        return ResponseEntity.ok("Resource accessed successfully");
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<String> handleRateLimitException(RateLimitException ex) {
        return ResponseEntity
                .status(429)
                .body("Too Many Requests: " + ex.getMessage());
    }
}

