package com.example.marketsupplier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private final long windowSizeMs;
    private final int maxRequestsPerWindow;
    private final int maxRequestsPerMinute;
    private final int maxRequestsPerHour;

    public RateLimitService(
            @Value("${app.rate-limit.window-size-ms:60000}") long windowSizeMs,
            @Value("${app.rate-limit.max-per-window:100}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.max-per-minute:60}") int maxRequestsPerMinute,
            @Value("${app.rate-limit.max-per-hour:1000}") int maxRequestsPerHour) {
        this.windowSizeMs = windowSizeMs;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxRequestsPerHour = maxRequestsPerHour;
    }

    public boolean isAllowed(String identifier) {
        return isAllowed(identifier, maxRequestsPerWindow);
    }

    public boolean isAllowed(String identifier, int maxRequests) {
        long currentTime = System.currentTimeMillis();
        RateLimitInfo info = rateLimitMap.computeIfAbsent(identifier, k -> new RateLimitInfo());

        // Clean old entries
        cleanupOldEntries(currentTime);

        // Check if within limits
        if (info.getRequestCount(currentTime, windowSizeMs) >= maxRequests) {
            return false;
        }

        // Increment counter
        info.incrementRequest(currentTime);
        return true;
    }

    public boolean isAllowedPerMinute(String identifier) {
        return isAllowed(identifier, maxRequestsPerMinute);
    }

    public boolean isAllowedPerHour(String identifier) {
        return isAllowed(identifier, maxRequestsPerHour);
    }

    public RateLimitInfo getRateLimitInfo(String identifier) {
        return rateLimitMap.get(identifier);
    }

    public void resetRateLimit(String identifier) {
        rateLimitMap.remove(identifier);
    }

    private void cleanupOldEntries(long currentTime) {
        rateLimitMap.entrySet().removeIf(entry -> {
            RateLimitInfo info = entry.getValue();
            return info.getRequestCount(currentTime, windowSizeMs) == 0 && 
                   currentTime - info.getLastRequestTime() > windowSizeMs * 2;
        });
    }

    public static class RateLimitInfo {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicLong lastRequestTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        public void incrementRequest(long currentTime) {
            // Reset window if needed
            if (currentTime - windowStart.get() >= 60000) { // 1 minute window
                windowStart.set(currentTime);
                requestCount.set(0);
            }
            requestCount.incrementAndGet();
            lastRequestTime.set(currentTime);
        }

        public int getRequestCount(long currentTime, long windowSizeMs) {
            if (currentTime - windowStart.get() >= windowSizeMs) {
                return 0; // Window expired
            }
            return requestCount.get();
        }

        public long getLastRequestTime() {
            return lastRequestTime.get();
        }

        public long getWindowStart() {
            return windowStart.get();
        }
    }

    public static class RateLimitException extends RuntimeException {
        private final String identifier;
        private final int retryAfterSeconds;

        public RateLimitException(String identifier, int retryAfterSeconds) {
            super("Rate limit exceeded for " + identifier + ". Retry after " + retryAfterSeconds + " seconds");
            this.identifier = identifier;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public String getIdentifier() {
            return identifier;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
