package com.example.marketsupplier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class CircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);

    @Value("${app.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${app.circuit-breaker.timeout-duration:60}")
    private long timeoutDurationSeconds;

    @Value("${app.circuit-breaker.retry-timeout:30}")
    private long retryTimeoutSeconds;

    private final ConcurrentHashMap<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();

    public enum CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, calls fail fast
        HALF_OPEN  // Testing if service is back
    }

    public static class CircuitBreakerState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private volatile CircuitState state = CircuitState.CLOSED;
        private final AtomicInteger successCount = new AtomicInteger(0);

        public int getFailureCount() { return failureCount.get(); }
        public void incrementFailure() { failureCount.incrementAndGet(); }
        public void resetFailure() { failureCount.set(0); }
        
        public long getLastFailureTime() { return lastFailureTime.get(); }
        public void setLastFailureTime(long time) { lastFailureTime.set(time); }
        
        public CircuitState getState() { return state; }
        public void setState(CircuitState state) { this.state = state; }
        
        public int getSuccessCount() { return successCount.get(); }
        public void incrementSuccess() { successCount.incrementAndGet(); }
        public void resetSuccess() { successCount.set(0); }
    }

    public <T> T execute(String serviceName, Supplier<T> operation, Supplier<T> fallback) {
        CircuitBreakerState state = getOrCreateState(serviceName);
        
        if (state.getState() == CircuitState.OPEN) {
            if (shouldAttemptReset(state)) {
                state.setState(CircuitState.HALF_OPEN);
                log.info("Circuit breaker for {} moved to HALF_OPEN state", serviceName);
            } else {
                log.warn("Circuit breaker for {} is OPEN, using fallback", serviceName);
                return fallback.get();
            }
        }

        try {
            T result = operation.get();
            onSuccess(state, serviceName);
            return result;
        } catch (Exception e) {
            onFailure(state, serviceName, e);
            log.warn("Operation failed for {}, using fallback", serviceName, e);
            return fallback.get();
        }
    }

    public void execute(String serviceName, Runnable operation, Runnable fallback) {
        CircuitBreakerState state = getOrCreateState(serviceName);
        
        if (state.getState() == CircuitState.OPEN) {
            if (shouldAttemptReset(state)) {
                state.setState(CircuitState.HALF_OPEN);
                log.info("Circuit breaker for {} moved to HALF_OPEN state", serviceName);
            } else {
                log.warn("Circuit breaker for {} is OPEN, using fallback", serviceName);
                fallback.run();
                return;
            }
        }

        try {
            operation.run();
            onSuccess(state, serviceName);
        } catch (Exception e) {
            onFailure(state, serviceName, e);
            log.warn("Operation failed for {}, using fallback", serviceName, e);
            fallback.run();
        }
    }

    public boolean isCircuitOpen(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        return state != null && state.getState() == CircuitState.OPEN;
    }

    public boolean isCircuitHalfOpen(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        return state != null && state.getState() == CircuitState.HALF_OPEN;
    }

    public boolean isCircuitClosed(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        return state == null || state.getState() == CircuitState.CLOSED;
    }

    public CircuitBreakerState getCircuitState(String serviceName) {
        return circuitStates.get(serviceName);
    }

    public void resetCircuit(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        if (state != null) {
            state.setState(CircuitState.CLOSED);
            state.resetFailure();
            state.resetSuccess();
            log.info("Circuit breaker for {} has been reset", serviceName);
        }
    }

    public void forceOpenCircuit(String serviceName) {
        CircuitBreakerState state = getOrCreateState(serviceName);
        state.setState(CircuitState.OPEN);
        state.setLastFailureTime(System.currentTimeMillis());
        log.warn("Circuit breaker for {} has been forced OPEN", serviceName);
    }

    public void forceCloseCircuit(String serviceName) {
        CircuitBreakerState state = getOrCreateState(serviceName);
        state.setState(CircuitState.CLOSED);
        state.resetFailure();
        state.resetSuccess();
        log.info("Circuit breaker for {} has been forced CLOSED", serviceName);
    }

    public CircuitBreakerStats getStats(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        if (state == null) {
            return new CircuitBreakerStats(serviceName, CircuitState.CLOSED, 0, 0, 0);
        }
        
        return new CircuitBreakerStats(
            serviceName,
            state.getState(),
            state.getFailureCount(),
            state.getSuccessCount(),
            state.getLastFailureTime()
        );
    }

    public java.util.Map<String, CircuitBreakerStats> getAllStats() {
        java.util.Map<String, CircuitBreakerStats> stats = new java.util.HashMap<>();
        for (String serviceName : circuitStates.keySet()) {
            stats.put(serviceName, getStats(serviceName));
        }
        return stats;
    }

    private CircuitBreakerState getOrCreateState(String serviceName) {
        return circuitStates.computeIfAbsent(serviceName, k -> new CircuitBreakerState());
    }

    private void onSuccess(CircuitBreakerState state, String serviceName) {
        state.incrementSuccess();
        
        if (state.getState() == CircuitState.HALF_OPEN) {
            // If we're in half-open and got a success, close the circuit
            state.setState(CircuitState.CLOSED);
            state.resetFailure();
            state.resetSuccess();
            log.info("Circuit breaker for {} moved to CLOSED state after success", serviceName);
        } else if (state.getState() == CircuitState.CLOSED) {
            // Reset failure count on success in closed state
            state.resetFailure();
        }
    }

    private void onFailure(CircuitBreakerState state, String serviceName, Exception e) {
        state.incrementFailure();
        state.setLastFailureTime(System.currentTimeMillis());
        
        if (state.getState() == CircuitState.HALF_OPEN) {
            // If we're in half-open and got a failure, open the circuit
            state.setState(CircuitState.OPEN);
            log.warn("Circuit breaker for {} moved to OPEN state after failure in HALF_OPEN", serviceName);
        } else if (state.getState() == CircuitState.CLOSED && state.getFailureCount() >= failureThreshold) {
            // If we're in closed state and reached failure threshold, open the circuit
            state.setState(CircuitState.OPEN);
            log.warn("Circuit breaker for {} moved to OPEN state after {} failures", serviceName, failureThreshold);
        }
    }

    private boolean shouldAttemptReset(CircuitBreakerState state) {
        long timeSinceLastFailure = System.currentTimeMillis() - state.getLastFailureTime();
        return timeSinceLastFailure >= (retryTimeoutSeconds * 1000);
    }

    public static class CircuitBreakerStats {
        private final String serviceName;
        private final CircuitState state;
        private final int failureCount;
        private final int successCount;
        private final long lastFailureTime;

        public CircuitBreakerStats(String serviceName, CircuitState state, int failureCount, int successCount, long lastFailureTime) {
            this.serviceName = serviceName;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastFailureTime = lastFailureTime;
        }

        public String getServiceName() { return serviceName; }
        public CircuitState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public long getLastFailureTime() { return lastFailureTime; }

        @Override
        public String toString() {
            return String.format("CircuitBreakerStats{service='%s', state=%s, failures=%d, successes=%d, lastFailure=%d}", 
                serviceName, state, failureCount, successCount, lastFailureTime);
        }
    }
}
