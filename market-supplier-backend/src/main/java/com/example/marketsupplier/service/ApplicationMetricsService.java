package com.example.marketsupplier.service;

import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.util.LoggerUtility;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApplicationMetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ConfigService configService;

    @Autowired
    private LoggerUtility loggerUtility;

    // Request Metrics
    @Autowired
    private Counter requestCounter;

    @Autowired
    private Counter requestErrorCounter;

    @Autowired
    private Timer requestTimer;

    // WhatsApp Metrics
    @Autowired
    private Counter whatsappMessageCounter;

    @Autowired
    private Counter whatsappMessageErrorCounter;

    @Autowired
    private Timer whatsappMessageTimer;

    // AI Agent Metrics
    @Autowired
    private Counter aiAgentRequestCounter;

    @Autowired
    private Counter aiAgentErrorCounter;

    @Autowired
    private Timer aiAgentTimer;

    @Autowired
    private Counter aiAgentFallbackCounter;

    // Database Metrics
    @Autowired
    private Counter dbQueryCounter;

    @Autowired
    private Counter dbQueryErrorCounter;

    @Autowired
    private Timer dbQueryTimer;

    // Cache Metrics
    @Autowired
    private Counter cacheHitCounter;

    @Autowired
    private Counter cacheMissCounter;

    @Autowired
    private Counter cacheEvictionCounter;

    @Autowired
    private Timer cacheTimer;

    // Redis Metrics
    @Autowired
    private Counter redisOperationCounter;

    @Autowired
    private Counter redisOperationErrorCounter;

    @Autowired
    private Timer redisOperationTimer;

    // Async Processing Metrics
    @Autowired
    private Counter asyncTaskCounter;

    @Autowired
    private Counter asyncTaskErrorCounter;

    @Autowired
    private Timer asyncTaskTimer;

    @Autowired
    private AtomicLong asyncTaskQueueSize;

    // Circuit Breaker Metrics
    @Autowired
    private Counter circuitBreakerOpenCounter;

    @Autowired
    private Counter circuitBreakerCloseCounter;

    @Autowired
    private Counter circuitBreakerHalfOpenCounter;

    // Rate Limiting Metrics
    @Autowired
    private Counter rateLimitExceededCounter;

    @Autowired
    private Counter rateLimitAllowedCounter;

    // Validation Metrics
    @Autowired
    private Counter validationErrorCounter;

    @Autowired
    private Counter validationSuccessCounter;

    // Business Metrics
    @Autowired
    private Counter orderCreatedCounter;

    @Autowired
    private Counter orderCancelledCounter;

    @Autowired
    private Counter cartItemAddedCounter;

    @Autowired
    private Counter cartItemRemovedCounter;

    // System Metrics
    @Autowired
    private AtomicLong activeConnections;

    @Autowired
    private AtomicLong memoryUsage;

    @Autowired
    private AtomicLong cpuUsage;

    // Health Check Metrics
    @Autowired
    private Counter healthCheckSuccessCounter;

    @Autowired
    private Counter healthCheckFailureCounter;

    @Autowired
    private Timer healthCheckTimer;

    // Error Metrics
    @Autowired
    private Counter errorCounter;

    @Autowired
    private Counter warningCounter;

    @Autowired
    private Counter infoCounter;

    // Performance Metrics
    @Autowired
    private DistributionSummary responseSizeDistribution;

    @Autowired
    private DistributionSummary requestSizeDistribution;

    @Autowired
    private DistributionSummary payloadSizeDistribution;

    // Custom Metrics Registry
    @Autowired
    private ConcurrentHashMap<String, Object> customMetrics;

    // Request Metrics
    public void recordRequest(String method, String endpoint, int statusCode, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_REQUEST")
            .withMetadata("method", method)
            .withMetadata("endpoint", endpoint)
            .withMetadata("status_code", statusCode)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("http.requests.total",
                    "method", method,
                    "endpoint", endpoint,
                    "status_code", String.valueOf(statusCode)
            ).increment();

            requestTimer.record(Duration.ofMillis(durationMs));

            if (statusCode >= 400) {
                meterRegistry.counter("http.requests.errors",
                        "method", method,
                        "endpoint", endpoint,
                        "status_code", String.valueOf(statusCode)
                ).increment();
            }

            loggerUtility.logInfo("Request metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record request metrics", context, e);
        }
    }

    // WhatsApp Metrics
    public void recordWhatsAppMessage(String messageType, String status, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_WHATSAPP_MESSAGE")
            .withMetadata("message_type", messageType)
            .withMetadata("status", status)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("whatsapp.messages.total",
                    "message_type", messageType,
                    "status", status
            ).increment();

            whatsappMessageTimer.record(Duration.ofMillis(durationMs));

            if ("error".equals(status)) {
                meterRegistry.counter("whatsapp.messages.errors",
                        "message_type", messageType,
                        "status", status
                ).increment();
            }

            loggerUtility.logInfo("WhatsApp message metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record WhatsApp message metrics", context, e);
        }
    }

    // AI Agent Metrics
    public void recordAIAgentRequest(String intent, String status, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_AI_AGENT_REQUEST")
            .withMetadata("intent", intent)
            .withMetadata("status", status)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("ai.agent.requests.total",
                    "intent", intent,
                    "status", status
            ).increment();

            aiAgentTimer.record(Duration.ofMillis(durationMs));

            if ("error".equals(status)) {
                meterRegistry.counter("ai.agent.errors",
                        "intent", intent,
                        "status", status
                ).increment();
            }

            loggerUtility.logInfo("AI agent request metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record AI agent request metrics", context, e);
        }
    }

    public void recordAIAgentFallback(String reason) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_AI_AGENT_FALLBACK")
            .withMetadata("reason", reason);

        try {
            meterRegistry.counter("ai.agent.fallback.total", "reason", reason).increment();

            loggerUtility.logInfo("AI agent fallback metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record AI agent fallback metrics", context, e);
        }
    }

    // Database Metrics
    public void recordDatabaseQuery(String queryType, String status, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_DATABASE_QUERY")
            .withMetadata("query_type", queryType)
            .withMetadata("status", status)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("db.query.total", "query_type", queryType, "status", status).increment();

            dbQueryTimer.record(Duration.ofMillis(durationMs));

            if ("error".equals(status)) {
                meterRegistry.counter("db.query.error.total", "query_type", queryType, "status", status).increment();
            }

            loggerUtility.logInfo("Database query metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record database query metrics", context, e);
        }
    }

    // Cache Metrics
    public void recordCacheHit(String cacheType, String key) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CACHE_HIT")
            .withMetadata("cache_type", cacheType)
            .withMetadata("key", key);

        try {
            meterRegistry.counter("cache.hit.total", "cache_type", cacheType).increment();

            loggerUtility.logInfo("Cache hit metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cache hit metrics", context, e);
        }
    }

    public void recordCacheMiss(String cacheType, String key) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CACHE_MISS")
            .withMetadata("cache_type", cacheType)
            .withMetadata("key", key);

        try {
            meterRegistry.counter("cache.miss.total", "cache_type", cacheType).increment();

            loggerUtility.logInfo("Cache miss metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cache miss metrics", context, e);
        }
    }

    public void recordCacheEviction(String cacheType, String key) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CACHE_EVICTION")
            .withMetadata("cache_type", cacheType)
            .withMetadata("key", key);

        try {
            meterRegistry.counter("cache.eviction.total", "cache_type", cacheType).increment();

            loggerUtility.logInfo("Cache eviction metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cache eviction metrics", context, e);
        }
    }

    public void recordCacheOperation(String operation, String cacheType, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CACHE_OPERATION")
            .withMetadata("operation", operation)
            .withMetadata("cache_type", cacheType)
            .withMetadata("duration_ms", durationMs);

        try {
            cacheTimer.record(Duration.ofMillis(durationMs));

            loggerUtility.logInfo("Cache operation metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cache operation metrics", context, e);
        }
    }

    // Redis Metrics
    public void recordRedisOperation(String operation, String status, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_REDIS_OPERATION")
            .withMetadata("operation", operation)
            .withMetadata("status", status)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("redis.operation.total", "operation", operation, "status", status).increment();

            redisOperationTimer.record(Duration.ofMillis(durationMs));

            if ("error".equals(status)) {
                meterRegistry.counter("redis.operation.error.total", "operation", operation, "status", status).increment();
            }

            loggerUtility.logInfo("Redis operation metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record Redis operation metrics", context, e);
        }
    }

    // Async Processing Metrics
    public void recordAsyncTask(String taskType, String status, long durationMs) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_ASYNC_TASK")
            .withMetadata("task_type", taskType)
            .withMetadata("status", status)
            .withMetadata("duration_ms", durationMs);

        try {
            meterRegistry.counter("async.task.total", "task_type", taskType, "status", status).increment();

            asyncTaskTimer.record(Duration.ofMillis(durationMs));

            if ("error".equals(status)) {
                meterRegistry.counter("async.task.error.total", "task_type", taskType, "status", status).increment();
            }

            loggerUtility.logInfo("Async task metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record async task metrics", context, e);
        }
    }

    public void updateAsyncTaskQueueSize(long size) {
        asyncTaskQueueSize.set(size);
    }

    // Circuit Breaker Metrics
    public void recordCircuitBreakerOpen(String service) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CIRCUIT_BREAKER_OPEN")
            .withMetadata("service", service);

        try {
            meterRegistry.counter("circuitBreakerOpenCounter").increment();

            loggerUtility.logInfo("Circuit breaker open metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record circuit breaker open metrics", context, e);
        }
    }

    public void recordCircuitBreakerClose(String service) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CIRCUIT_BREAKER_CLOSE")
            .withMetadata("service", service);

        try {
            meterRegistry.counter("circuitBreakerCloseCounter").increment();

            loggerUtility.logInfo("Circuit breaker close metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record circuit breaker close metrics", context, e);
        }
    }

    public void recordCircuitBreakerHalfOpen(String service) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CIRCUIT_BREAKER_HALF_OPEN")
            .withMetadata("service", service);

        try {
            meterRegistry.counter("circuitBreakerHalfOpenCounter").increment();

            loggerUtility.logInfo("Circuit breaker half open metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record circuit breaker half open metrics", context, e);
        }
    }

    // Rate Limiting Metrics
    public void recordRateLimitExceeded(String clientId, String endpoint) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_RATE_LIMIT_EXCEEDED")
            .withMetadata("client_id", clientId)
            .withMetadata("endpoint", endpoint);

        try {
            meterRegistry.counter("rateLimitExceededCounter").increment();

            loggerUtility.logInfo("Rate limit exceeded metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record rate limit exceeded metrics", context, e);
        }
    }

    public void recordRateLimitAllowed(String clientId, String endpoint) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_RATE_LIMIT_ALLOWED")
            .withMetadata("client_id", clientId)
            .withMetadata("endpoint", endpoint);

        try {
            meterRegistry.counter("rateLimitAllowedCounter").increment();

            loggerUtility.logInfo("Rate limit allowed metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record rate limit allowed metrics", context, e);
        }
    }

    // Validation Metrics
    public void recordValidationError(String field, String errorType) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_VALIDATION_ERROR")
            .withMetadata("field", field)
            .withMetadata("error_type", errorType);

        try {
            meterRegistry.counter("validationErrorCounter").increment();

            loggerUtility.logInfo("Validation error metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record validation error metrics", context, e);
        }
    }

    public void recordValidationSuccess(String field) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_VALIDATION_SUCCESS")
            .withMetadata("field", field);

        try {
            meterRegistry.counter("validationSuccessCounter").increment();

            loggerUtility.logInfo("Validation success metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record validation success metrics", context, e);
        }
    }

    // Business Metrics
    public void recordOrderCreated(String orderType, String status) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_ORDER_CREATED")
            .withMetadata("order_type", orderType)
            .withMetadata("status", status);

        try {
            meterRegistry.counter("orderCreatedCounter").increment();

            loggerUtility.logInfo("Order created metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record order created metrics", context, e);
        }
    }

    public void recordOrderCancelled(String orderType, String reason) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_ORDER_CANCELLED")
            .withMetadata("order_type", orderType)
            .withMetadata("reason", reason);

        try {
            meterRegistry.counter("orderCancelledCounter").increment();

            loggerUtility.logInfo("Order cancelled metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record order cancelled metrics", context, e);
        }
    }

    public void recordCartItemAdded(String itemType, String quantity) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CART_ITEM_ADDED")
            .withMetadata("item_type", itemType)
            .withMetadata("quantity", quantity);

        try {
            meterRegistry.counter("cartItemAddedCounter").increment();

            loggerUtility.logInfo("Cart item added metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cart item added metrics", context, e);
        }
    }

    public void recordCartItemRemoved(String itemType, String quantity) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CART_ITEM_REMOVED")
            .withMetadata("item_type", itemType)
            .withMetadata("quantity", quantity);

        try {
            meterRegistry.counter("cartItemRemovedCounter").increment();

            loggerUtility.logInfo("Cart item removed metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record cart item removed metrics", context, e);
        }
    }

    // System Metrics
    public void updateActiveConnections(long count) {
        activeConnections.set(count);
    }

    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }

    public void updateCpuUsage(long percent) {
        cpuUsage.set(percent);
    }

    // Health Check Metrics
    public void recordHealthCheckSuccess(String checkType) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_HEALTH_CHECK_SUCCESS")
            .withMetadata("check_type", checkType);

        try {
            meterRegistry.counter("healthCheckSuccessCounter").increment();

            loggerUtility.logInfo("Health check success metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record health check success metrics", context, e);
        }
    }

    public void recordHealthCheckFailure(String checkType, String error) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_HEALTH_CHECK_FAILURE")
            .withMetadata("check_type", checkType)
            .withMetadata("error", error);

        try {
            meterRegistry.counter("healthCheckFailureCounter").increment();

            loggerUtility.logInfo("Health check failure metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record health check failure metrics", context, e);
        }
    }

    // Error Metrics
    public void recordError(String errorType, String component) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_ERROR")
            .withMetadata("error_type", errorType)
            .withMetadata("component", component);

        try {
            meterRegistry.counter("errorCounter").increment();

            loggerUtility.logInfo("Error metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record error metrics", context, e);
        }
    }

    public void recordWarning(String warningType, String component) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_WARNING")
            .withMetadata("warning_type", warningType)
            .withMetadata("component", component);

        try {
            meterRegistry.counter("warningCounter").increment();

            loggerUtility.logInfo("Warning metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record warning metrics", context, e);
        }
    }

    public void recordInfo(String infoType, String component) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_INFO")
            .withMetadata("info_type", infoType)
            .withMetadata("component", component);

        try {
            meterRegistry.counter("infoCounter").increment();

            loggerUtility.logInfo("Info metrics recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record info metrics", context, e);
        }
    }

    // Performance Metrics
    public void recordResponseSize(long bytes) {
        responseSizeDistribution.record(bytes);
    }

    public void recordRequestSize(long bytes) {
        requestSizeDistribution.record(bytes);
    }

    public void recordPayloadSize(long bytes) {
        payloadSizeDistribution.record(bytes);
    }

    // Custom Metrics
    public void recordCustomMetric(String metricName, String value, Map<String, String> tags) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CUSTOM_METRIC")
            .withMetadata("metric_name", metricName)
            .withMetadata("value", value)
            .withMetadata("tags", tags);

        try {
            // Convert Map to varargs for tags
            String[] tagArray = new String[tags.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                tagArray[i++] = entry.getKey();
                tagArray[i++] = entry.getValue();
            }
            
            Counter.builder(metricName)
                    .description("Custom metric: " + metricName)
                    .tags(tagArray)
                    .register(meterRegistry)
                    .increment(Double.parseDouble(value));

            loggerUtility.logInfo("Custom metric recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record custom metric", context, e);
        }
    }

    public void recordCustomTimer(String timerName, long durationMs, Map<String, String> tags) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CUSTOM_TIMER")
            .withMetadata("timer_name", timerName)
            .withMetadata("duration_ms", durationMs)
            .withMetadata("tags", tags);

        try {
            // Convert Map to varargs for tags
            String[] tagArray = new String[tags.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                tagArray[i++] = entry.getKey();
                tagArray[i++] = entry.getValue();
            }
            
            Timer.builder(timerName)
                    .description("Custom timer: " + timerName)
                    .tags(tagArray)
                    .register(meterRegistry)
                    .record(Duration.ofMillis(durationMs));

            loggerUtility.logInfo("Custom timer recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record custom timer", context, e);
        }
    }

    public void recordCustomGauge(String gaugeName, long value, Map<String, String> tags) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RECORD_CUSTOM_GAUGE")
            .withMetadata("gauge_name", gaugeName)
            .withMetadata("value", value)
            .withMetadata("tags", tags);

        try {
            // Convert Map to varargs for tags
            String[] tagArray = new String[tags.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                tagArray[i++] = entry.getKey();
                tagArray[i++] = entry.getValue();
            }
            
            Gauge.builder(gaugeName, () -> value)
                    .description("Custom gauge: " + gaugeName)
                    .tags(tagArray)
                    .register(meterRegistry);

            loggerUtility.logInfo("Custom gauge recorded", context);
        } catch (Exception e) {
            loggerUtility.logError("Failed to record custom gauge", context, e);
        }
    }

    // Utility Methods
    public void updateSystemMetrics() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            updateMemoryUsage(usedMemory);

            // Update CPU usage (simplified)
            long cpuUsage = getCpuUsage();
            updateCpuUsage(cpuUsage);

            // Update active connections (simplified)
            long activeConnections = getActiveConnections();
            updateActiveConnections(activeConnections);

        } catch (Exception e) {
            loggerUtility.logError("Failed to update system metrics", LoggerUtility.LogContext.create("UPDATE_SYSTEM_METRICS"), e);
        }
    }

    private long getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return Math.round(osBean.getProcessCpuLoad() * 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private long getActiveConnections() {
        // Simplified implementation - in real scenario, this would come from connection pool
        return Thread.activeCount();
    }
}
