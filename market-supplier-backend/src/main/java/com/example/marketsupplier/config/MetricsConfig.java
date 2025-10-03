package com.example.marketsupplier.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    @Autowired
    private MeterRegistry meterRegistry;


    // Request Metrics
    @Bean
    public Counter requestCounter() {
        return Counter.builder("app_requests_total")
                .description("Total number of requests")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter requestErrorCounter() {
        return Counter.builder("app_requests_errors_total")
                .description("Total number of request errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer requestTimer() {
        return Timer.builder("app_request_duration_seconds")
                .description("Request duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // WhatsApp Metrics
    @Bean
    public Counter whatsappMessageCounter() {
        return Counter.builder("app_whatsapp_messages_total")
                .description("Total number of WhatsApp messages processed")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter whatsappMessageErrorCounter() {
        return Counter.builder("app_whatsapp_messages_errors_total")
                .description("Total number of WhatsApp message errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer whatsappMessageTimer() {
        return Timer.builder("app_whatsapp_message_duration_seconds")
                .description("WhatsApp message processing duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // AI Agent Metrics
    @Bean
    public Counter aiAgentRequestCounter() {
        return Counter.builder("app_ai_agent_requests_total")
                .description("Total number of AI agent requests")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter aiAgentErrorCounter() {
        return Counter.builder("app_ai_agent_errors_total")
                .description("Total number of AI agent errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer aiAgentTimer() {
        return Timer.builder("app_ai_agent_duration_seconds")
                .description("AI agent processing duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter aiAgentFallbackCounter() {
        return Counter.builder("app_ai_agent_fallbacks_total")
                .description("Total number of AI agent fallbacks")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Database Metrics
    @Bean
    public Counter dbQueryCounter() {
        return Counter.builder("app_db_queries_total")
                .description("Total number of database queries")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter dbQueryErrorCounter() {
        return Counter.builder("app_db_queries_errors_total")
                .description("Total number of database query errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer dbQueryTimer() {
        return Timer.builder("app_db_query_duration_seconds")
                .description("Database query duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Cache Metrics
    @Bean
    public Counter cacheHitCounter() {
        return Counter.builder("app_cache_hits_total")
                .description("Total number of cache hits")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter cacheMissCounter() {
        return Counter.builder("app_cache_misses_total")
                .description("Total number of cache misses")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter cacheEvictionCounter() {
        return Counter.builder("app_cache_evictions_total")
                .description("Total number of cache evictions")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer cacheTimer() {
        return Timer.builder("app_cache_duration_seconds")
                .description("Cache operation duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Redis Metrics
    @Bean
    public Counter redisOperationCounter() {
        return Counter.builder("app_redis_operations_total")
                .description("Total number of Redis operations")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter redisOperationErrorCounter() {
        return Counter.builder("app_redis_operations_errors_total")
                .description("Total number of Redis operation errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer redisOperationTimer() {
        return Timer.builder("app_redis_operation_duration_seconds")
                .description("Redis operation duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Async Processing Metrics
    @Bean
    public Counter asyncTaskCounter() {
        return Counter.builder("app_async_tasks_total")
                .description("Total number of async tasks")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter asyncTaskErrorCounter() {
        return Counter.builder("app_async_tasks_errors_total")
                .description("Total number of async task errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer asyncTaskTimer() {
        return Timer.builder("app_async_task_duration_seconds")
                .description("Async task duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public AtomicLong asyncTaskQueueSize() {
        return new AtomicLong(0);
    }

    @Bean
    public Gauge asyncTaskQueueSizeGauge() {
        return Gauge.builder("app_async_task_queue_size", asyncTaskQueueSize(), AtomicLong::get)
                .description("Current async task queue size")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Circuit Breaker Metrics
    @Bean
    public Counter circuitBreakerOpenCounter() {
        return Counter.builder("app_circuit_breaker_opens_total")
                .description("Total number of circuit breaker opens")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter circuitBreakerCloseCounter() {
        return Counter.builder("app_circuit_breaker_closes_total")
                .description("Total number of circuit breaker closes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter circuitBreakerHalfOpenCounter() {
        return Counter.builder("app_circuit_breaker_half_opens_total")
                .description("Total number of circuit breaker half opens")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Rate Limiting Metrics
    @Bean
    public Counter rateLimitExceededCounter() {
        return Counter.builder("app_rate_limit_exceeded_total")
                .description("Total number of rate limit exceeded")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter rateLimitAllowedCounter() {
        return Counter.builder("app_rate_limit_allowed_total")
                .description("Total number of rate limit allowed")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Validation Metrics
    @Bean
    public Counter validationErrorCounter() {
        return Counter.builder("app_validation_errors_total")
                .description("Total number of validation errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter validationSuccessCounter() {
        return Counter.builder("app_validation_success_total")
                .description("Total number of validation successes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Business Metrics
    @Bean
    public Counter orderCreatedCounter() {
        return Counter.builder("app_orders_created_total")
                .description("Total number of orders created")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter orderCancelledCounter() {
        return Counter.builder("app_orders_cancelled_total")
                .description("Total number of orders cancelled")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter cartItemAddedCounter() {
        return Counter.builder("app_cart_items_added_total")
                .description("Total number of cart items added")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter cartItemRemovedCounter() {
        return Counter.builder("app_cart_items_removed_total")
                .description("Total number of cart items removed")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // System Metrics
    @Bean
    public AtomicLong activeConnections() {
        return new AtomicLong(0);
    }

    @Bean
    public Gauge activeConnectionsGauge() {
        return Gauge.builder("app_active_connections", activeConnections(), AtomicLong::get)
                .description("Current number of active connections")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public AtomicLong memoryUsage() {
        return new AtomicLong(0);
    }

    @Bean
    public Gauge memoryUsageGauge() {
        return Gauge.builder("app_memory_usage_bytes", memoryUsage(), AtomicLong::get)
                .description("Current memory usage in bytes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public AtomicLong cpuUsage() {
        return new AtomicLong(0);
    }

    @Bean
    public Gauge cpuUsageGauge() {
        return Gauge.builder("app_cpu_usage_percent", cpuUsage(), AtomicLong::get)
                .description("Current CPU usage percentage")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Custom Metrics Registry
    @Bean
    public ConcurrentHashMap<String, Object> customMetrics() {
        return new ConcurrentHashMap<>();
    }

    // Health Check Metrics
    @Bean
    public AtomicLong healthCheckCounter() {
        return new AtomicLong(0);
    }

    @Bean
    public Counter healthCheckSuccessCounter() {
        return Counter.builder("app_health_checks_success_total")
                .description("Total number of successful health checks")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter healthCheckFailureCounter() {
        return Counter.builder("app_health_checks_failure_total")
                .description("Total number of failed health checks")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Timer healthCheckTimer() {
        return Timer.builder("app_health_check_duration_seconds")
                .description("Health check duration in seconds")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Error Metrics
    @Bean
    public Counter errorCounter() {
        return Counter.builder("app_errors_total")
                .description("Total number of errors")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter warningCounter() {
        return Counter.builder("app_warnings_total")
                .description("Total number of warnings")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public Counter infoCounter() {
        return Counter.builder("app_info_total")
                .description("Total number of info messages")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    // Performance Metrics
    @Bean
    public DistributionSummary responseSizeDistribution() {
        return DistributionSummary.builder("app_response_size_bytes")
                .description("Response size distribution in bytes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public DistributionSummary requestSizeDistribution() {
        return DistributionSummary.builder("app_request_size_bytes")
                .description("Request size distribution in bytes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }

    @Bean
    public DistributionSummary payloadSizeDistribution() {
        return DistributionSummary.builder("app_payload_size_bytes")
                .description("Payload size distribution in bytes")
                .tag("service", "market-supplier")
                .register(meterRegistry);
    }
}
