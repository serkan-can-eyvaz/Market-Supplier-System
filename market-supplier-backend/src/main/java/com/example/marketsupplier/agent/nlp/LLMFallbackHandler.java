package com.example.marketsupplier.agent.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.marketsupplier.agent.metrics.AgentMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LLMFallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(LLMFallbackHandler.class);

    @Value("${agent.llm.fallback.retry_attempts:2}")
    private int maxRetryAttempts;

    @Value("${agent.llm.fallback.retry_delay_ms:500}")
    private long retryDelayMs;

    private final Map<String, String> fallbackMessages = new HashMap<>();

    private final AgentMetrics metrics;

    public LLMFallbackHandler(AgentMetrics metrics) {
        this.metrics = metrics;
        initializeFallbackMessages();
    }

    private void initializeFallbackMessages() {
        fallbackMessages.put("llm_empty_response", "Şu an yanıt veremiyorum, lütfen tekrar dene");
        fallbackMessages.put("llm_invalid_json", "Anladığımı işleyemiyorum, lütfen farklı şekilde sor");
        fallbackMessages.put("llm_rate_limit", "Çok hızlı mesaj gönderiyorsun, biraz bekle");
        fallbackMessages.put("llm_timeout", "Yanıt vermem zaman aldı, tekrar dener misin?");
        fallbackMessages.put("llm_api_error", "AI servisinde sorun var, birazdan düzelir");
        fallbackMessages.put("system_error", "Sistem hatası oluştu, lütfen tekrar dene");
        fallbackMessages.put("external_service_error", "Dış servis hatası, lütfen tekrar dene");
        fallbackMessages.put("context_error", "Bağlam hatası, lütfen tekrar dene");
    }

    public String getFallbackMessage(String errorType, String originalError) {
        String baseMessage = fallbackMessages.getOrDefault(errorType, "Beklenmeyen hata oluştu");
        
        // Add help suggestion for certain error types
        if (shouldSuggestHelp(errorType)) {
            baseMessage += " Yardım için 'yardım' yazabilirsin.";
        }
        
        log.warn("LLM Fallback triggered - Type: {}, Original: {}", errorType, originalError);
        try { if (metrics != null) metrics.incLlmFallback(errorType); } catch (Exception ignored) {}
        return baseMessage;
    }

    public String getFallbackMessage(String errorType) {
        return getFallbackMessage(errorType, null);
    }

    public boolean shouldRetry(String errorType, int currentAttempt) {
        if (currentAttempt >= maxRetryAttempts) {
            return false;
        }
        
        // Retry for rate limits and timeouts
        return "llm_rate_limit".equals(errorType) || 
               "llm_timeout".equals(errorType) ||
               "llm_api_error".equals(errorType);
    }

    public long getRetryDelay(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = retryDelayMs * (1L << attempt);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 2);
        return baseDelay + jitter;
    }

    public String handleEmptyResponse(String originalPrompt) {
        log.warn("LLM returned empty response for prompt: {}", originalPrompt);
        return getFallbackMessage("llm_empty_response");
    }

    public String handleInvalidJson(String jsonResponse, String originalPrompt) {
        log.warn("LLM returned invalid JSON: {} for prompt: {}", jsonResponse, originalPrompt);
        return getFallbackMessage("llm_invalid_json");
    }

    public String handleRateLimit(int attempt) {
        log.warn("LLM rate limit hit, attempt: {}", attempt);
        return getFallbackMessage("llm_rate_limit");
    }

    public String handleTimeout() {
        log.warn("LLM request timeout");
        return getFallbackMessage("llm_timeout");
    }

    public String handleApiError(int statusCode, String responseBody) {
        log.error("LLM API error - Status: {}, Body: {}", statusCode, responseBody);
        return getFallbackMessage("llm_api_error");
    }

    private boolean shouldSuggestHelp(String errorType) {
        return "llm_empty_response".equals(errorType) ||
               "llm_invalid_json".equals(errorType) ||
               "system_error".equals(errorType) ||
               "context_error".equals(errorType);
    }
}
