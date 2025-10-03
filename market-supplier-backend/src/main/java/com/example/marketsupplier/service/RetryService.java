package com.example.marketsupplier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
public class RetryService {

    private static final Logger log = LoggerFactory.getLogger(RetryService.class);

    @Value("${app.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.retry.initial-delay:1000}")
    private long initialDelayMs;

    @Value("${app.retry.max-delay:10000}")
    private long maxDelayMs;

    @Value("${app.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.retry.jitter:true}")
    private boolean jitterEnabled;

    public enum RetryStrategy {
        FIXED,           // Fixed delay between retries
        EXPONENTIAL,     // Exponential backoff
        LINEAR,          // Linear backoff
        RANDOM           // Random delay
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private long maxDelayMs = 10000;
        private double multiplier = 2.0;
        private boolean jitterEnabled = true;
        private RetryStrategy strategy = RetryStrategy.EXPONENTIAL;
        private Class<? extends Exception>[] retryableExceptions = new Class[]{Exception.class};

        public RetryConfig maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RetryConfig initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public RetryConfig maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public RetryConfig multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public RetryConfig jitterEnabled(boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
            return this;
        }

        public RetryConfig strategy(RetryStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public RetryConfig retryableExceptions(Class<? extends Exception>... exceptions) {
            this.retryableExceptions = exceptions;
            return this;
        }

        // Getters
        public int getMaxAttempts() { return maxAttempts; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public double getMultiplier() { return multiplier; }
        public boolean isJitterEnabled() { return jitterEnabled; }
        public RetryStrategy getStrategy() { return strategy; }
        public Class<? extends Exception>[] getRetryableExceptions() { return retryableExceptions; }
    }

    public static class RetryResult<T> {
        private final T result;
        private final boolean success;
        private final int attempts;
        private final long totalDurationMs;
        private final Exception lastException;

        public RetryResult(T result, boolean success, int attempts, long totalDurationMs, Exception lastException) {
            this.result = result;
            this.success = success;
            this.attempts = attempts;
            this.totalDurationMs = totalDurationMs;
            this.lastException = lastException;
        }

        public T getResult() { return result; }
        public boolean isSuccess() { return success; }
        public int getAttempts() { return attempts; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public Exception getLastException() { return lastException; }
    }

    public <T> RetryResult<T> execute(Supplier<T> operation) {
        return execute(operation, new RetryConfig());
    }

    public <T> RetryResult<T> execute(Supplier<T> operation, RetryConfig config) {
        return execute(operation, config, null);
    }

    public <T> RetryResult<T> execute(Supplier<T> operation, Supplier<T> fallback) {
        return execute(operation, new RetryConfig(), fallback);
    }

    public <T> RetryResult<T> execute(Supplier<T> operation, RetryConfig config, Supplier<T> fallback) {
        LocalDateTime startTime = LocalDateTime.now();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            try {
                log.debug("Executing operation, attempt {}/{}", attempt, config.getMaxAttempts());
                T result = operation.get();
                
                if (attempt > 1) {
                    log.info("Operation succeeded on attempt {}", attempt);
                }
                
                long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
                return new RetryResult<>(result, true, attempt, duration, null);
                
            } catch (Exception e) {
                lastException = e;
                
                if (!isRetryableException(e, config.getRetryableExceptions())) {
                    log.warn("Non-retryable exception occurred: {}", e.getMessage());
                    break;
                }
                
                if (attempt == config.getMaxAttempts()) {
                    log.warn("Operation failed after {} attempts", config.getMaxAttempts(), e);
                    break;
                }
                
                long delay = calculateDelay(attempt, config);
                log.warn("Operation failed on attempt {}, retrying in {}ms: {}", attempt, delay, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted");
                    break;
                }
            }
        }
        
        long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
        
        if (fallback != null) {
            try {
                log.info("Using fallback after {} failed attempts", config.getMaxAttempts());
                T fallbackResult = fallback.get();
                return new RetryResult<>(fallbackResult, false, config.getMaxAttempts(), duration, lastException);
            } catch (Exception e) {
                log.error("Fallback also failed", e);
                return new RetryResult<>(null, false, config.getMaxAttempts(), duration, e);
            }
        }
        
        return new RetryResult<>(null, false, config.getMaxAttempts(), duration, lastException);
    }

    public void execute(Runnable operation) {
        execute(operation, new RetryConfig());
    }

    public void execute(Runnable operation, RetryConfig config) {
        execute(operation, config, null);
    }

    public void execute(Runnable operation, Runnable fallback) {
        execute(operation, new RetryConfig(), fallback);
    }

    public void execute(Runnable operation, RetryConfig config, Runnable fallback) {
        LocalDateTime startTime = LocalDateTime.now();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            try {
                log.debug("Executing operation, attempt {}/{}", attempt, config.getMaxAttempts());
                operation.run();
                
                if (attempt > 1) {
                    log.info("Operation succeeded on attempt {}", attempt);
                }
                return;
                
            } catch (Exception e) {
                lastException = e;
                
                if (!isRetryableException(e, config.getRetryableExceptions())) {
                    log.warn("Non-retryable exception occurred: {}", e.getMessage());
                    break;
                }
                
                if (attempt == config.getMaxAttempts()) {
                    log.warn("Operation failed after {} attempts", config.getMaxAttempts(), e);
                    break;
                }
                
                long delay = calculateDelay(attempt, config);
                log.warn("Operation failed on attempt {}, retrying in {}ms: {}", attempt, delay, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted");
                    break;
                }
            }
        }
        
        if (fallback != null) {
            try {
                log.info("Using fallback after {} failed attempts", config.getMaxAttempts());
                fallback.run();
            } catch (Exception e) {
                log.error("Fallback also failed", e);
                throw new RuntimeException("Operation and fallback both failed", e);
            }
        } else if (lastException != null) {
            throw new RuntimeException("Operation failed after " + config.getMaxAttempts() + " attempts", lastException);
        }
    }

    private boolean isRetryableException(Exception e, Class<? extends Exception>[] retryableExceptions) {
        for (Class<? extends Exception> exceptionClass : retryableExceptions) {
            if (exceptionClass.isAssignableFrom(e.getClass())) {
                return true;
            }
        }
        return false;
    }

    private long calculateDelay(int attempt, RetryConfig config) {
        long delay;
        
        switch (config.getStrategy()) {
            case FIXED:
                delay = config.getInitialDelayMs();
                break;
            case LINEAR:
                delay = config.getInitialDelayMs() * attempt;
                break;
            case EXPONENTIAL:
                delay = (long) (config.getInitialDelayMs() * Math.pow(config.getMultiplier(), attempt - 1));
                break;
            case RANDOM:
                delay = ThreadLocalRandom.current().nextLong(config.getInitialDelayMs(), config.getMaxDelayMs());
                break;
            default:
                delay = config.getInitialDelayMs();
        }
        
        // Apply jitter if enabled
        if (config.isJitterEnabled() && delay > 0) {
            long jitter = (long) (delay * 0.1 * ThreadLocalRandom.current().nextDouble(-1, 1));
            delay = Math.max(0, delay + jitter);
        }
        
        // Cap at max delay
        return Math.min(delay, config.getMaxDelayMs());
    }

    // Convenience methods for common scenarios
    public <T> RetryResult<T> executeWithExponentialBackoff(Supplier<T> operation) {
        return execute(operation, new RetryConfig().strategy(RetryStrategy.EXPONENTIAL));
    }

    public <T> RetryResult<T> executeWithFixedDelay(Supplier<T> operation) {
        return execute(operation, new RetryConfig().strategy(RetryStrategy.FIXED));
    }

    public <T> RetryResult<T> executeWithLinearBackoff(Supplier<T> operation) {
        return execute(operation, new RetryConfig().strategy(RetryStrategy.LINEAR));
    }

    public <T> RetryResult<T> executeWithRandomDelay(Supplier<T> operation) {
        return execute(operation, new RetryConfig().strategy(RetryStrategy.RANDOM));
    }

    // Quick retry for critical operations
    public <T> RetryResult<T> quickRetry(Supplier<T> operation) {
        return execute(operation, new RetryConfig()
            .maxAttempts(2)
            .initialDelayMs(500)
            .strategy(RetryStrategy.FIXED));
    }

    // Aggressive retry for non-critical operations
    public <T> RetryResult<T> aggressiveRetry(Supplier<T> operation) {
        return execute(operation, new RetryConfig()
            .maxAttempts(5)
            .initialDelayMs(2000)
            .maxDelayMs(30000)
            .strategy(RetryStrategy.EXPONENTIAL));
    }
}
