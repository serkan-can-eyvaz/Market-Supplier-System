package com.example.marketsupplier.controller;

import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.util.LoggerUtility;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private LoggerUtility loggerUtility;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Counter healthCheckSuccessCounter;

    @Autowired
    private Counter healthCheckFailureCounter;

    @Autowired
    private Timer healthCheckTimer;

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("LIVENESS_CHECK");
        loggerUtility.logInfo("Liveness check requested", context);

        Timer.Sample sample = Timer.start();
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("version", "1.0.0");
            response.put("uptime", getUptime());
            response.put("environment", configService.getActiveProfile());

            // Basic application health checks
            response.put("application", checkApplicationHealth());
            response.put("configuration", checkConfigurationHealth());

            healthCheckSuccessCounter.increment();
            sample.stop(healthCheckTimer);

            loggerUtility.logInfo("Liveness check successful", context);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            healthCheckFailureCounter.increment();
            sample.stop(healthCheckTimer);
            
            loggerUtility.logError("Liveness check failed", context, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("READINESS_CHECK");
        loggerUtility.logInfo("Readiness check requested", context);

        Timer.Sample sample = Timer.start();
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("version", "1.0.0");
            response.put("uptime", getUptime());
            response.put("environment", configService.getActiveProfile());

            // Comprehensive readiness checks
            response.put("application", checkApplicationHealth());
            response.put("configuration", checkConfigurationHealth());
            response.put("database", checkDatabaseHealth());
            response.put("redis", checkRedisHealth());
            response.put("external_services", checkExternalServicesHealth());

            healthCheckSuccessCounter.increment();
            sample.stop(healthCheckTimer);

            loggerUtility.logInfo("Readiness check successful", context);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            healthCheckFailureCounter.increment();
            sample.stop(healthCheckTimer);
            
            loggerUtility.logError("Readiness check failed", context, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/startup")
    public ResponseEntity<Map<String, Object>> startup() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("STARTUP_CHECK");
        loggerUtility.logInfo("Startup check requested", context);

        Timer.Sample sample = Timer.start();
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("version", "1.0.0");
            response.put("uptime", getUptime());
            response.put("environment", configService.getActiveProfile());

            // Startup-specific checks
            response.put("application", checkApplicationHealth());
            response.put("configuration", checkConfigurationHealth());
            response.put("database", checkDatabaseHealth());
            response.put("redis", checkRedisHealth());
            response.put("external_services", checkExternalServicesHealth());
            response.put("feature_flags", checkFeatureFlagsHealth());

            healthCheckSuccessCounter.increment();
            sample.stop(healthCheckTimer);

            loggerUtility.logInfo("Startup check successful", context);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            healthCheckFailureCounter.increment();
            sample.stop(healthCheckTimer);
            
            loggerUtility.logError("Startup check failed", context, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("timestamp", LocalDateTime.now());
            response.put("service", "market-supplier");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("INFO_CHECK");
        loggerUtility.logInfo("Info check requested", context);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "market-supplier");
        response.put("version", "1.0.0");
        response.put("uptime", getUptime());
        response.put("environment", configService.getActiveProfile());
        response.put("java_version", System.getProperty("java.version"));
        response.put("os_name", System.getProperty("os.name"));
        response.put("os_version", System.getProperty("os.version"));
        response.put("available_processors", Runtime.getRuntime().availableProcessors());
        response.put("max_memory", Runtime.getRuntime().maxMemory());
        response.put("total_memory", Runtime.getRuntime().totalMemory());
        response.put("free_memory", Runtime.getRuntime().freeMemory());
        response.put("used_memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> checkApplicationHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("thread_count", Thread.activeCount());
            health.put("memory_usage", getMemoryUsage());
            health.put("cpu_usage", getCpuUsage());
            health.put("gc_info", getGcInfo());
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkConfigurationHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("active_profile", configService.getActiveProfile());
            health.put("feature_flags", configService.getAllFeatureFlags());
            health.put("secrets_available", checkSecretsAvailability());
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                long duration = System.currentTimeMillis() - startTime;
                
                if (isValid) {
                    health.put("status", "UP");
                    health.put("connection_time_ms", duration);
                    health.put("database_product", connection.getMetaData().getDatabaseProductName());
                    health.put("database_version", connection.getMetaData().getDatabaseProductVersion());
                } else {
                    health.put("status", "DOWN");
                    health.put("error", "Database connection is not valid");
                }
            }
        } catch (SQLException e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            long duration = System.currentTimeMillis() - startTime;
            
            if ("PONG".equals(pong)) {
                health.put("status", "UP");
                health.put("ping_time_ms", duration);
                health.put("redis_enabled", configService.isRedisEnabled());
            } else {
                health.put("status", "DOWN");
                health.put("error", "Redis ping failed");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("redis_enabled", configService.isRedisEnabled());
        }
        return health;
    }

    private Map<String, Object> checkExternalServicesHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("whatsapp_enabled", configService.isWhatsappEnabled());
            health.put("ai_agent_enabled", configService.isAiAgentEnabled());
            health.put("monitoring_enabled", configService.isMonitoringEnabled());
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkFeatureFlagsHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("feature_flags", configService.getAllFeatureFlags());
            health.put("critical_features", checkCriticalFeatures());
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkCriticalFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("ai_agent", configService.isAiAgentEnabled());
        features.put("whatsapp", configService.isWhatsappEnabled());
        features.put("redis", configService.isRedisEnabled());
        features.put("validation", configService.isValidationEnabled());
        features.put("rate_limiting", configService.isRateLimitingEnabled());
        return features;
    }

    private Map<String, Object> checkSecretsAvailability() {
        Map<String, Object> secrets = new HashMap<>();
        secrets.put("whatsapp_access_token", configService.isSecretAvailable("app.whatsapp.access_token"));
        secrets.put("whatsapp_verify_token", configService.isSecretAvailable("app.whatsapp.verify_token"));
        secrets.put("jwt_secret", configService.isSecretAvailable("app.jwt.secret"));
        secrets.put("admin_bootstrap_token", configService.isSecretAvailable("app.admin.bootstrap-token"));
        return secrets;
    }

    private long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    private Map<String, Object> getMemoryUsage() {
        Map<String, Object> memory = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        memory.put("max", runtime.maxMemory());
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("used_percent", ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100);
        return memory;
    }

    private Map<String, Object> getCpuUsage() {
        Map<String, Object> cpu = new HashMap<>();
        cpu.put("available_processors", Runtime.getRuntime().availableProcessors());
        cpu.put("load_average", getSystemLoadAverage());
        return cpu;
    }

    private Map<String, Object> getGcInfo() {
        Map<String, Object> gc = new HashMap<>();
        gc.put("total_collections", getTotalGcCollections());
        gc.put("total_time_ms", getTotalGcTime());
        return gc;
    }

    private double getSystemLoadAverage() {
        try {
            return com.sun.management.OperatingSystemMXBean.class.cast(
                java.lang.management.ManagementFactory.getOperatingSystemMXBean()
            ).getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0;
        }
    }

    private long getTotalGcCollections() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount())
                .sum();
    }

    private long getTotalGcTime() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionTime())
                .sum();
    }

    private static final long startTime = System.currentTimeMillis();
}
