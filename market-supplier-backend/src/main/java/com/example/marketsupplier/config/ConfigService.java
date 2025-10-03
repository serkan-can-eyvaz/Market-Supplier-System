package com.example.marketsupplier.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    @Autowired
    private Environment environment;

    // Feature Flags
    @Value("${app.features.ai-agent.enabled:true}")
    private boolean aiAgentEnabled;

    @Value("${app.features.whatsapp.enabled:true}")
    private boolean whatsappEnabled;

    @Value("${app.features.redis.enabled:true}")
    private boolean redisEnabled;

    @Value("${app.features.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${app.features.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${app.features.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${app.features.logging.enabled:true}")
    private boolean loggingEnabled;

    @Value("${app.features.caching.enabled:true}")
    private boolean cachingEnabled;

    @Value("${app.features.async-processing.enabled:true}")
    private boolean asyncProcessingEnabled;

    // AI Configuration
    @Value("${app.ai.provider:openai}")
    private String aiProvider;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String aiModel;

    @Value("${app.ai.max-tokens:2000}")
    private int aiMaxTokens;

    @Value("${app.ai.temperature:0.3}")
    private double aiTemperature;

    @Value("${app.ai.timeout:20000}")
    private long aiTimeout;

    @Value("${OPENAI_API_KEY:}")
    private String aiApiKey;

    @Value("${ai.api.base_url:https://api.openai.com/v1}")
    private String aiApiBaseUrl;

    // WhatsApp Configuration
    @Value("${app.whatsapp.access_token:}")
    private String whatsappAccessToken;

    @Value("${app.whatsapp.phone_number_id:}")
    private String whatsappPhoneNumberId;

    @Value("${app.whatsapp.verify_token:}")
    private String whatsappVerifyToken;

    @Value("${app.whatsapp.webhook_url:}")
    private String whatsappWebhookUrl;

    // JWT Configuration
    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration:86400000}")
    private long jwtRefreshExpiration;

    // Rate Limiting Configuration
    @Value("${app.rate-limit.window-size-ms:60000}")
    private long rateLimitWindowSizeMs;

    @Value("${app.rate-limit.max-per-window:100}")
    private int rateLimitMaxPerWindow;

    @Value("${app.rate-limit.max-per-minute:60}")
    private int rateLimitMaxPerMinute;

    @Value("${app.rate-limit.max-per-hour:1000}")
    private int rateLimitMaxPerHour;

    // Circuit Breaker Configuration
    @Value("${app.circuit-breaker.failure-threshold:5}")
    private int circuitBreakerFailureThreshold;

    @Value("${app.circuit-breaker.timeout-duration:60}")
    private long circuitBreakerTimeoutDuration;

    @Value("${app.circuit-breaker.retry-timeout:30}")
    private long circuitBreakerRetryTimeout;

    // Retry Configuration
    @Value("${app.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${app.retry.initial-delay:1000}")
    private long retryInitialDelay;

    @Value("${app.retry.max-delay:10000}")
    private long retryMaxDelay;

    @Value("${app.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.retry.jitter:true}")
    private boolean retryJitter;

    // Cache Configuration
    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.default-ttl:300}")
    private long cacheDefaultTtl;

    @Value("${app.cache.local-fallback:true}")
    private boolean cacheLocalFallback;

    @Value("${app.cache.redis.enabled:true}")
    private boolean cacheRedisEnabled;

    // Async Configuration
    @Value("${app.async.core-pool-size:5}")
    private int asyncCorePoolSize;

    @Value("${app.async.max-pool-size:20}")
    private int asyncMaxPoolSize;

    @Value("${app.async.queue-capacity:100}")
    private int asyncQueueCapacity;

    @Value("${app.async.thread-name-prefix:Async-}")
    private String asyncThreadNamePrefix;

    // Resilience Configuration
    @Value("${app.resilience.llm.enabled:true}")
    private boolean resilienceLlmEnabled;

    @Value("${app.resilience.redis.enabled:true}")
    private boolean resilienceRedisEnabled;

    @Value("${app.resilience.whatsapp.enabled:true}")
    private boolean resilienceWhatsappEnabled;

    // Security Configuration
    @Value("${app.security.input-sanitization.enabled:true}")
    private boolean securityInputSanitizationEnabled;

    @Value("${app.security.rate-limiting.enabled:true}")
    private boolean securityRateLimitingEnabled;

    @Value("${app.security.jwt-validation.enabled:true}")
    private boolean securityJwtValidationEnabled;

    // Monitoring Configuration
    @Value("${app.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${app.monitoring.metrics.enabled:true}")
    private boolean monitoringMetricsEnabled;

    @Value("${app.monitoring.health-check.enabled:true}")
    private boolean monitoringHealthCheckEnabled;

    @Value("${app.monitoring.actuator.enabled:true}")
    private boolean monitoringActuatorEnabled;

    // Admin Configuration
    @Value("${app.admin.bootstrap-token:}")
    private String adminBootstrapToken;

    @Value("${app.admin.enabled:false}")
    private boolean adminEnabled;

    public String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "default";
    }

    public boolean isFeatureEnabled(String featureName) {
        String propertyName = "app.features." + featureName + ".enabled";
        return environment.getProperty(propertyName, Boolean.class, false);
    }

    public boolean isFeatureEnabled(String featureName, boolean defaultValue) {
        String propertyName = "app.features." + featureName + ".enabled";
        return environment.getProperty(propertyName, Boolean.class, defaultValue);
    }

    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }

    public long getLongProperty(String key, long defaultValue) {
        return environment.getProperty(key, Long.class, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    public double getDoubleProperty(String key, double defaultValue) {
        return environment.getProperty(key, Double.class, defaultValue);
    }

    public String getSecret(String secretName) {
        String secret = environment.getProperty(secretName);
        if (secret == null || secret.isEmpty()) {
            log.warn("Secret {} not found or empty", secretName);
            return null;
        }
        return secret;
    }

    public String getSecret(String secretName, String defaultValue) {
        String secret = environment.getProperty(secretName);
        return secret != null && !secret.isEmpty() ? secret : defaultValue;
    }

    public boolean isSecretAvailable(String secretName) {
        String secret = environment.getProperty(secretName);
        return secret != null && !secret.isEmpty();
    }

    public Map<String, Object> getAllFeatureFlags() {
        Map<String, Object> flags = new HashMap<>();
        flags.put("ai-agent", aiAgentEnabled);
        flags.put("whatsapp", whatsappEnabled);
        flags.put("redis", redisEnabled);
        flags.put("circuit-breaker", circuitBreakerEnabled);
        flags.put("rate-limiting", rateLimitingEnabled);
        flags.put("validation", validationEnabled);
        flags.put("logging", loggingEnabled);
        flags.put("caching", cachingEnabled);
        flags.put("async-processing", asyncProcessingEnabled);
        return flags;
    }

    public Map<String, Object> getAiConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", aiProvider);
        config.put("model", aiModel);
        config.put("maxTokens", aiMaxTokens);
        config.put("temperature", aiTemperature);
        config.put("timeout", aiTimeout);
        return config;
    }

    public Map<String, Object> getWhatsappConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("accessToken", whatsappAccessToken != null && !whatsappAccessToken.isEmpty() ? "***" : null);
        config.put("phoneNumberId", whatsappPhoneNumberId);
        config.put("verifyToken", whatsappVerifyToken != null && !whatsappVerifyToken.isEmpty() ? "***" : null);
        config.put("webhookUrl", whatsappWebhookUrl);
        return config;
    }

    public Map<String, Object> getJwtConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("secret", jwtSecret != null && !jwtSecret.isEmpty() ? "***" : null);
        config.put("expiration", jwtExpiration);
        config.put("refreshExpiration", jwtRefreshExpiration);
        return config;
    }

    public Map<String, Object> getRateLimitConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("windowSizeMs", rateLimitWindowSizeMs);
        config.put("maxPerWindow", rateLimitMaxPerWindow);
        config.put("maxPerMinute", rateLimitMaxPerMinute);
        config.put("maxPerHour", rateLimitMaxPerHour);
        return config;
    }

    public Map<String, Object> getCircuitBreakerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("failureThreshold", circuitBreakerFailureThreshold);
        config.put("timeoutDuration", circuitBreakerTimeoutDuration);
        config.put("retryTimeout", circuitBreakerRetryTimeout);
        return config;
    }

    public Map<String, Object> getRetryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxAttempts", retryMaxAttempts);
        config.put("initialDelay", retryInitialDelay);
        config.put("maxDelay", retryMaxDelay);
        config.put("multiplier", retryMultiplier);
        config.put("jitter", retryJitter);
        return config;
    }

    public Map<String, Object> getCacheConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", cacheEnabled);
        config.put("defaultTtl", cacheDefaultTtl);
        config.put("localFallback", cacheLocalFallback);
        config.put("redisEnabled", cacheRedisEnabled);
        return config;
    }

    public Map<String, Object> getAsyncConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("corePoolSize", asyncCorePoolSize);
        config.put("maxPoolSize", asyncMaxPoolSize);
        config.put("queueCapacity", asyncQueueCapacity);
        config.put("threadNamePrefix", asyncThreadNamePrefix);
        return config;
    }

    public Map<String, Object> getResilienceConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("llmEnabled", resilienceLlmEnabled);
        config.put("redisEnabled", resilienceRedisEnabled);
        config.put("whatsappEnabled", resilienceWhatsappEnabled);
        return config;
    }

    public Map<String, Object> getSecurityConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("inputSanitizationEnabled", securityInputSanitizationEnabled);
        config.put("rateLimitingEnabled", securityRateLimitingEnabled);
        config.put("jwtValidationEnabled", securityJwtValidationEnabled);
        return config;
    }

    public Map<String, Object> getMonitoringConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", monitoringEnabled);
        config.put("metricsEnabled", monitoringMetricsEnabled);
        config.put("healthCheckEnabled", monitoringHealthCheckEnabled);
        config.put("actuatorEnabled", monitoringActuatorEnabled);
        return config;
    }

    public Map<String, Object> getAdminConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrapToken", adminBootstrapToken != null && !adminBootstrapToken.isEmpty() ? "***" : null);
        config.put("enabled", adminEnabled);
        return config;
    }

    public Map<String, Object> getAllConfig() {
        Map<String, Object> allConfig = new HashMap<>();
        allConfig.put("activeProfile", getActiveProfile());
        allConfig.put("featureFlags", getAllFeatureFlags());
        allConfig.put("ai", getAiConfig());
        allConfig.put("whatsapp", getWhatsappConfig());
        allConfig.put("jwt", getJwtConfig());
        allConfig.put("rateLimit", getRateLimitConfig());
        allConfig.put("circuitBreaker", getCircuitBreakerConfig());
        allConfig.put("retry", getRetryConfig());
        allConfig.put("cache", getCacheConfig());
        allConfig.put("async", getAsyncConfig());
        allConfig.put("resilience", getResilienceConfig());
        allConfig.put("security", getSecurityConfig());
        allConfig.put("monitoring", getMonitoringConfig());
        allConfig.put("admin", getAdminConfig());
        return allConfig;
    }

    public boolean isDevelopment() {
        return "dev".equals(getActiveProfile());
    }

    public boolean isStaging() {
        return "staging".equals(getActiveProfile());
    }

    public boolean isProduction() {
        return "prod".equals(getActiveProfile());
    }

    public void logConfiguration() {
        log.info("Configuration loaded for profile: {}", getActiveProfile());
        log.info("Feature flags: {}", getAllFeatureFlags());
        log.info("AI config: {}", getAiConfig());
        log.info("WhatsApp config: {}", getWhatsappConfig());
        log.info("JWT config: {}", getJwtConfig());
        log.info("Rate limit config: {}", getRateLimitConfig());
        log.info("Circuit breaker config: {}", getCircuitBreakerConfig());
        log.info("Retry config: {}", getRetryConfig());
        log.info("Cache config: {}", getCacheConfig());
        log.info("Async config: {}", getAsyncConfig());
        log.info("Resilience config: {}", getResilienceConfig());
        log.info("Security config: {}", getSecurityConfig());
        log.info("Monitoring config: {}", getMonitoringConfig());
        log.info("Admin config: {}", getAdminConfig());
    }

    // Getters for direct access
    public boolean isAiAgentEnabled() { return aiAgentEnabled; }
    public boolean isWhatsappEnabled() { return whatsappEnabled; }
    public boolean isRedisEnabled() { return redisEnabled; }
    public boolean isCircuitBreakerEnabled() { return circuitBreakerEnabled; }
    public boolean isRateLimitingEnabled() { return rateLimitingEnabled; }
    public boolean isValidationEnabled() { return validationEnabled; }
    public boolean isLoggingEnabled() { return loggingEnabled; }
    public boolean isCachingEnabled() { return cachingEnabled; }
    public boolean isAsyncProcessingEnabled() { return asyncProcessingEnabled; }

    public String getAiProvider() { return aiProvider; }
    public String getAiModel() { return aiModel; }
    public int getAiMaxTokens() { return aiMaxTokens; }
    public double getAiTemperature() { return aiTemperature; }
    public long getAiTimeout() { return aiTimeout; }
    public String getAiApiKey() { return aiApiKey; }
    public String getAiApiBaseUrl() { return aiApiBaseUrl; }

    public String getWhatsappAccessToken() { return whatsappAccessToken; }
    public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
    public String getWhatsappVerifyToken() { return whatsappVerifyToken; }
    public String getWhatsappWebhookUrl() { return whatsappWebhookUrl; }

    public String getJwtSecret() { return jwtSecret; }
    public long getJwtExpiration() { return jwtExpiration; }
    public long getJwtRefreshExpiration() { return jwtRefreshExpiration; }

    public long getRateLimitWindowSizeMs() { return rateLimitWindowSizeMs; }
    public int getRateLimitMaxPerWindow() { return rateLimitMaxPerWindow; }
    public int getRateLimitMaxPerMinute() { return rateLimitMaxPerMinute; }
    public int getRateLimitMaxPerHour() { return rateLimitMaxPerHour; }

    public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
    public long getCircuitBreakerTimeoutDuration() { return circuitBreakerTimeoutDuration; }
    public long getCircuitBreakerRetryTimeout() { return circuitBreakerRetryTimeout; }

    public int getRetryMaxAttempts() { return retryMaxAttempts; }
    public long getRetryInitialDelay() { return retryInitialDelay; }
    public long getRetryMaxDelay() { return retryMaxDelay; }
    public double getRetryMultiplier() { return retryMultiplier; }
    public boolean isRetryJitter() { return retryJitter; }

    public boolean isCacheEnabled() { return cacheEnabled; }
    public long getCacheDefaultTtl() { return cacheDefaultTtl; }
    public boolean isCacheLocalFallback() { return cacheLocalFallback; }
    public boolean isCacheRedisEnabled() { return cacheRedisEnabled; }

    public int getAsyncCorePoolSize() { return asyncCorePoolSize; }
    public int getAsyncMaxPoolSize() { return asyncMaxPoolSize; }
    public int getAsyncQueueCapacity() { return asyncQueueCapacity; }
    public String getAsyncThreadNamePrefix() { return asyncThreadNamePrefix; }

    public boolean isResilienceLlmEnabled() { return resilienceLlmEnabled; }
    public boolean isResilienceRedisEnabled() { return resilienceRedisEnabled; }
    public boolean isResilienceWhatsappEnabled() { return resilienceWhatsappEnabled; }

    public boolean isSecurityInputSanitizationEnabled() { return securityInputSanitizationEnabled; }
    public boolean isSecurityRateLimitingEnabled() { return securityRateLimitingEnabled; }
    public boolean isSecurityJwtValidationEnabled() { return securityJwtValidationEnabled; }

    public boolean isMonitoringEnabled() { return monitoringEnabled; }
    public boolean isMonitoringMetricsEnabled() { return monitoringMetricsEnabled; }
    public boolean isMonitoringHealthCheckEnabled() { return monitoringHealthCheckEnabled; }
    public boolean isMonitoringActuatorEnabled() { return monitoringActuatorEnabled; }

    public String getAdminBootstrapToken() { return adminBootstrapToken; }
    public boolean isAdminEnabled() { return adminEnabled; }
}
