# Redis-based Rate Limiter Implementation
A high-performance, distributed rate limiter implementation using Redis and Java. This project provides a robust solution for API rate limiting with a sliding window algorithm.

### 🚀Features

1. Sliding window algorithm for accurate rate limiting
2. Redis-backed for distributed systems
3. Thread-safe implementation
4. Configurable time windows and request limits
5. Spring Boot integration example
6. Comprehensive test coverage

### 🔧 Technical Stack

1. Java 11+
2. Redis 6.x+
3. Jedis Client 4.3.1
4. Spring Boot (optional for web integration)
5. JUnit 5 for testing

### 📖 Implementation Details
The rate limiter uses Redis Sorted Sets to implement a sliding window algorithm. Each request is tracked with a timestamp, and older requests are automatically pruned based on the configured time window.

### 🧪 Test Cases

1. Basic Rate Limiting Tests

- This test verifies the fundamental rate limiting behavior
- It attempts to make 6 requests with a limit of 5 requests
- The first 5 requests should succeed (return true)
- The 6th request should be denied (return false)
- This ensures the basic counting mechanism works correctly

2. Time Window Tests

- This tests the expiration of the time window
- It sets a very short window (1 second) with a limit of 2 requests
- Makes 2 requests which should succeed
- The 3rd request immediately after should fail
- Then waits for the 1-second window to expire
- After expiration, a new request should succeed
- This verifies that limits properly reset after the time window passes

3. Concurrent Access Tests

- Tests how the rate limiter handles multiple simultaneous requests
- Creates 10 threads that simultaneously make 1000 total requests
- Sets a limit of 1000 requests per minute
- Verifies that exactly 1000 requests succeed
- This ensures thread safety and accurate counting even under high concurrency
- Important for real-world scenarios where many requests arrive simultaneously

4. Multi-User Tests

- Verifies that different users have separate rate limits
- Creates two users, each with a limit of 2 requests
- Makes 2 requests for each user (all should succeed)
- Makes a 3rd request for each user (all should fail)
- This confirms that users don't interfere with each other's limits
- Essential for multi-tenant applications
