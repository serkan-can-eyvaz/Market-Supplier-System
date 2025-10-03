package com.example.marketsupplier.controller;

import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.util.LoggerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private LoggerUtility loggerUtility;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getActiveProfile() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_ACTIVE_PROFILE");
        loggerUtility.logInfo("Getting active profile", context);

        Map<String, Object> response = Map.of(
            "activeProfile", configService.getActiveProfile(),
            "isDevelopment", configService.isDevelopment(),
            "isStaging", configService.isStaging(),
            "isProduction", configService.isProduction()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getAllFeatureFlags() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_FEATURE_FLAGS");
        loggerUtility.logInfo("Getting all feature flags", context);

        Map<String, Object> response = Map.of(
            "featureFlags", configService.getAllFeatureFlags(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/features/{featureName}")
    public ResponseEntity<Map<String, Object>> getFeatureFlag(@PathVariable String featureName) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_FEATURE_FLAG")
            .withMetadata("feature_name", featureName);
        loggerUtility.logInfo("Getting feature flag", context);

        boolean isEnabled = configService.isFeatureEnabled(featureName);
        Map<String, Object> response = Map.of(
            "featureName", featureName,
            "enabled", isEnabled,
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> getAiConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_AI_CONFIG");
        loggerUtility.logInfo("Getting AI configuration", context);

        Map<String, Object> response = Map.of(
            "aiConfig", configService.getAiConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<Map<String, Object>> getWhatsappConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_WHATSAPP_CONFIG");
        loggerUtility.logInfo("Getting WhatsApp configuration", context);

        Map<String, Object> response = Map.of(
            "whatsappConfig", configService.getWhatsappConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jwt")
    public ResponseEntity<Map<String, Object>> getJwtConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_JWT_CONFIG");
        loggerUtility.logInfo("Getting JWT configuration", context);

        Map<String, Object> response = Map.of(
            "jwtConfig", configService.getJwtConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimitConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_RATE_LIMIT_CONFIG");
        loggerUtility.logInfo("Getting rate limit configuration", context);

        Map<String, Object> response = Map.of(
            "rateLimitConfig", configService.getRateLimitConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/circuit-breaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_CIRCUIT_BREAKER_CONFIG");
        loggerUtility.logInfo("Getting circuit breaker configuration", context);

        Map<String, Object> response = Map.of(
            "circuitBreakerConfig", configService.getCircuitBreakerConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/retry")
    public ResponseEntity<Map<String, Object>> getRetryConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_RETRY_CONFIG");
        loggerUtility.logInfo("Getting retry configuration", context);

        Map<String, Object> response = Map.of(
            "retryConfig", configService.getRetryConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_CACHE_CONFIG");
        loggerUtility.logInfo("Getting cache configuration", context);

        Map<String, Object> response = Map.of(
            "cacheConfig", configService.getCacheConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/async")
    public ResponseEntity<Map<String, Object>> getAsyncConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_ASYNC_CONFIG");
        loggerUtility.logInfo("Getting async configuration", context);

        Map<String, Object> response = Map.of(
            "asyncConfig", configService.getAsyncConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/resilience")
    public ResponseEntity<Map<String, Object>> getResilienceConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_RESILIENCE_CONFIG");
        loggerUtility.logInfo("Getting resilience configuration", context);

        Map<String, Object> response = Map.of(
            "resilienceConfig", configService.getResilienceConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/security")
    public ResponseEntity<Map<String, Object>> getSecurityConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_SECURITY_CONFIG");
        loggerUtility.logInfo("Getting security configuration", context);

        Map<String, Object> response = Map.of(
            "securityConfig", configService.getSecurityConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monitoring")
    public ResponseEntity<Map<String, Object>> getMonitoringConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_MONITORING_CONFIG");
        loggerUtility.logInfo("Getting monitoring configuration", context);

        Map<String, Object> response = Map.of(
            "monitoringConfig", configService.getMonitoringConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAdminConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_ADMIN_CONFIG");
        loggerUtility.logInfo("Getting admin configuration", context);

        Map<String, Object> response = Map.of(
            "adminConfig", configService.getAdminConfig(),
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GET_ALL_CONFIG");
        loggerUtility.logInfo("Getting all configuration", context);

        Map<String, Object> response = configService.getAllConfig();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/secrets/check")
    public ResponseEntity<Map<String, Object>> checkSecrets() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("CHECK_SECRETS");
        loggerUtility.logInfo("Checking secrets availability", context);

        Map<String, Object> secrets = Map.of(
            "whatsappAccessToken", configService.isSecretAvailable("app.whatsapp.access_token"),
            "whatsappVerifyToken", configService.isSecretAvailable("app.whatsapp.verify_token"),
            "jwtSecret", configService.isSecretAvailable("app.jwt.secret"),
            "adminBootstrapToken", configService.isSecretAvailable("app.admin.bootstrap-token")
        );

        Map<String, Object> response = Map.of(
            "secrets", secrets,
            "activeProfile", configService.getActiveProfile()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfig() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RELOAD_CONFIG");
        loggerUtility.logInfo("Reloading configuration", context);

        // Log current configuration
        configService.logConfiguration();

        Map<String, Object> response = Map.of(
            "message", "Configuration reloaded successfully",
            "activeProfile", configService.getActiveProfile(),
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(response);
    }
}
