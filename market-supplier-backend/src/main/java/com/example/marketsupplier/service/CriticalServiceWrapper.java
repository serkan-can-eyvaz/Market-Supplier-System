package com.example.marketsupplier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class CriticalServiceWrapper {

    private static final Logger log = LoggerFactory.getLogger(CriticalServiceWrapper.class);

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Autowired
    private RetryService retryService;

    @Autowired
    private CacheService cacheService;

    @Value("${app.resilience.llm.enabled:true}")
    private boolean llmResilienceEnabled;

    @Value("${app.resilience.redis.enabled:true}")
    private boolean redisResilienceEnabled;

    @Value("${app.resilience.whatsapp.enabled:true}")
    private boolean whatsappResilienceEnabled;

    @Value("${ai.retry.max_attempts:3}")
    private int maxAttempts;

    @Value("${ai.retry.initial_backoff_ms:1000}")
    private long initialBackoffMs;

    // LLM Service Wrapper
    public String executeLlmOperation(Supplier<String> llmOperation) {
        if (!llmResilienceEnabled) {
            return llmOperation.get();
        }

        return circuitBreakerService.execute("llm-service", llmOperation, () -> {
            log.warn("LLM service fallback activated");
            return "Üzgünüm, şu anda AI servisimiz geçici olarak kullanılamıyor. Lütfen daha sonra tekrar deneyin.";
        });
    }

    // LLM operasyonlarını yeniden deneme mekanizması ile sar
    public <T> T executeLlmOperationWithRetry(Supplier<T> operation) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429) {
                    handleRetry("Rate limit exceeded", attempt);
                } else {
                    log.error("LLM operation failed with HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw e; // Diğer HTTP hatalarını yeniden fırlat
                }
            } catch (ResourceAccessException e) {
                handleRetry("Timeout or network issue", attempt);
            } catch (Exception e) {
                log.error("An unexpected error occurred during LLM operation", e);
                throw e; // Beklenmedik hataları yeniden fırlat
            }
        }
        log.error("LLM operation failed after {} attempts.", maxAttempts);
        return null; // Veya bir fallback değeri döndür
    }

    private void handleRetry(String reason, int attempt) {
        if (attempt < maxAttempts) {
            long backoff = calculateBackoffMs(reason, attempt);
            log.warn("{} on attempt {}. Retrying in {} ms...", reason, attempt, backoff);
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private long calculateBackoffMs(String reason, int attempt) {
        // Rate limit için özel bekleme süresi - çok daha uzun
        if (reason.contains("Rate limit")) {
            // Rate limit için: 5s, 15s, 30s, 60s, 120s
            return Math.min(5000L * (long) Math.pow(2, attempt - 1), 120000L);
        }
        
        // Diğer hatalar için normal exponential backoff
        return Math.min(initialBackoffMs * (long) Math.pow(2, attempt - 1), 30000L);
    }

    // Redis Service Wrapper
    public <T> T executeRedisOperation(String operationName, Supplier<T> redisOperation, Supplier<T> fallback) {
        if (!redisResilienceEnabled) {
            return redisOperation.get();
        }

        return circuitBreakerService.execute("redis-service", redisOperation, fallback);
    }

    public <T> T executeRedisOperationWithRetry(String operationName, Supplier<T> redisOperation, Supplier<T> fallback) {
        if (!redisResilienceEnabled) {
            return redisOperation.get();
        }

        RetryService.RetryResult<T> result = retryService.executeWithExponentialBackoff(() -> {
            return circuitBreakerService.execute("redis-service", redisOperation, () -> {
                throw new RuntimeException("Redis service unavailable");
            });
        });

        if (result.isSuccess()) {
            return result.getResult();
        } else {
            log.warn("Redis operation {} failed after {} attempts, using fallback", operationName, result.getAttempts());
            return fallback.get();
        }
    }

    // WhatsApp Service Wrapper
    public void executeWhatsAppOperation(String phone, String message, Runnable whatsappOperation) {
        if (!whatsappResilienceEnabled) {
            whatsappOperation.run();
            return;
        }

        circuitBreakerService.execute("whatsapp-service", whatsappOperation, () -> {
            log.warn("WhatsApp service fallback activated for phone: {}", phone);
            // Could implement alternative notification method here
        });
    }

    public void executeWhatsAppOperationWithRetry(String phone, String message, Runnable whatsappOperation) {
        if (!whatsappResilienceEnabled) {
            whatsappOperation.run();
            return;
        }

        retryService.executeWithExponentialBackoff(() -> {
            circuitBreakerService.execute("whatsapp-service", whatsappOperation, () -> {
                throw new RuntimeException("WhatsApp service unavailable");
            });
            return null; // void operation
        });
    }

    // Database Service Wrapper
    public <T> T executeDatabaseOperation(String operationName, Supplier<T> dbOperation, Supplier<T> fallback) {
        return circuitBreakerService.execute("database-service", dbOperation, fallback);
    }

    public <T> T executeDatabaseOperationWithRetry(String operationName, Supplier<T> dbOperation, Supplier<T> fallback) {
        RetryService.RetryResult<T> result = retryService.executeWithExponentialBackoff(() -> {
            return circuitBreakerService.execute("database-service", dbOperation, () -> {
                throw new RuntimeException("Database service unavailable");
            });
        });

        if (result.isSuccess()) {
            return result.getResult();
        } else {
            log.warn("Database operation {} failed after {} attempts, using fallback", operationName, result.getAttempts());
            return fallback.get();
        }
    }

    // Cache Service Wrapper
    public <T> T executeCacheOperation(String operationName, Supplier<T> cacheOperation, Supplier<T> fallback) {
        return circuitBreakerService.execute("cache-service", cacheOperation, fallback);
    }

    public <T> T executeCacheOperationWithRetry(String operationName, Supplier<T> cacheOperation, Supplier<T> fallback) {
        RetryService.RetryResult<T> result = retryService.executeWithExponentialBackoff(() -> {
            return circuitBreakerService.execute("cache-service", cacheOperation, () -> {
                throw new RuntimeException("Cache service unavailable");
            });
        });

        if (result.isSuccess()) {
            return result.getResult();
        } else {
            log.warn("Cache operation {} failed after {} attempts, using fallback", operationName, result.getAttempts());
            return fallback.get();
        }
    }

    // Async Operations
    public CompletableFuture<String> executeLlmOperationAsync(Supplier<String> llmOperation) {
        return CompletableFuture.supplyAsync(() -> executeLlmOperationWithRetry(llmOperation));
    }

    public CompletableFuture<Void> executeWhatsAppOperationAsync(String phone, String message, Runnable whatsappOperation) {
        return CompletableFuture.runAsync(() -> executeWhatsAppOperationWithRetry(phone, message, whatsappOperation));
    }

    public <T> CompletableFuture<T> executeDatabaseOperationAsync(String operationName, Supplier<T> dbOperation, Supplier<T> fallback) {
        return CompletableFuture.supplyAsync(() -> executeDatabaseOperationWithRetry(operationName, dbOperation, fallback));
    }

    public <T> CompletableFuture<T> executeCacheOperationAsync(String operationName, Supplier<T> cacheOperation, Supplier<T> fallback) {
        return CompletableFuture.supplyAsync(() -> executeCacheOperationWithRetry(operationName, cacheOperation, fallback));
    }

    // Health Check Methods
    public boolean isLlmServiceHealthy() {
        return circuitBreakerService.isCircuitClosed("llm-service");
    }

    public boolean isRedisServiceHealthy() {
        return circuitBreakerService.isCircuitClosed("redis-service");
    }

    public boolean isWhatsAppServiceHealthy() {
        return circuitBreakerService.isCircuitClosed("whatsapp-service");
    }

    public boolean isDatabaseServiceHealthy() {
        return circuitBreakerService.isCircuitClosed("database-service");
    }

    public boolean isCacheServiceHealthy() {
        return circuitBreakerService.isCircuitClosed("cache-service");
    }

    // Service Status
    public ServiceHealthStatus getServiceHealthStatus() {
        return new ServiceHealthStatus(
            isLlmServiceHealthy(),
            isRedisServiceHealthy(),
            isWhatsAppServiceHealthy(),
            isDatabaseServiceHealthy(),
            isCacheServiceHealthy()
        );
    }

    // Circuit Breaker Management
    public void resetAllCircuits() {
        circuitBreakerService.resetCircuit("llm-service");
        circuitBreakerService.resetCircuit("redis-service");
        circuitBreakerService.resetCircuit("whatsapp-service");
        circuitBreakerService.resetCircuit("database-service");
        circuitBreakerService.resetCircuit("cache-service");
        log.info("All circuit breakers have been reset");
    }

    public void forceOpenCircuit(String serviceName) {
        circuitBreakerService.forceOpenCircuit(serviceName);
        log.warn("Circuit breaker for {} has been forced open", serviceName);
    }

    public void forceCloseCircuit(String serviceName) {
        circuitBreakerService.forceCloseCircuit(serviceName);
        log.info("Circuit breaker for {} has been forced closed", serviceName);
    }

    // Statistics
    public java.util.Map<String, CircuitBreakerService.CircuitBreakerStats> getAllCircuitStats() {
        return circuitBreakerService.getAllStats();
    }

    public CircuitBreakerService.CircuitBreakerStats getCircuitStats(String serviceName) {
        return circuitBreakerService.getStats(serviceName);
    }

    // Service Health Status DTO
    public static class ServiceHealthStatus {
        private final boolean llmHealthy;
        private final boolean redisHealthy;
        private final boolean whatsappHealthy;
        private final boolean databaseHealthy;
        private final boolean cacheHealthy;

        public ServiceHealthStatus(boolean llmHealthy, boolean redisHealthy, boolean whatsappHealthy, 
                                 boolean databaseHealthy, boolean cacheHealthy) {
            this.llmHealthy = llmHealthy;
            this.redisHealthy = redisHealthy;
            this.whatsappHealthy = whatsappHealthy;
            this.databaseHealthy = databaseHealthy;
            this.cacheHealthy = cacheHealthy;
        }

        public boolean isLlmHealthy() { return llmHealthy; }
        public boolean isRedisHealthy() { return redisHealthy; }
        public boolean isWhatsappHealthy() { return whatsappHealthy; }
        public boolean isDatabaseHealthy() { return databaseHealthy; }
        public boolean isCacheHealthy() { return cacheHealthy; }

        public boolean isAllHealthy() {
            return llmHealthy && redisHealthy && whatsappHealthy && databaseHealthy && cacheHealthy;
        }

        public int getHealthyServiceCount() {
            int count = 0;
            if (llmHealthy) count++;
            if (redisHealthy) count++;
            if (whatsappHealthy) count++;
            if (databaseHealthy) count++;
            if (cacheHealthy) count++;
            return count;
        }

        @Override
        public String toString() {
            return String.format("ServiceHealthStatus{llm=%s, redis=%s, whatsapp=%s, database=%s, cache=%s}", 
                llmHealthy, redisHealthy, whatsappHealthy, databaseHealthy, cacheHealthy);
        }
    }
}
