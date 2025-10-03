package com.example.marketsupplier.security;

import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.util.LoggerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuth2Service {

    @Autowired
    private ConfigService configService;

    @Autowired
    private LoggerUtility loggerUtility;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuditLogService auditLogService;

    public OAuth2TokenResponse getWhatsAppAccessToken() {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("OAUTH2_WHATSAPP_TOKEN")
            .withMetadata("service", "whatsapp");
        
        loggerUtility.logInfo("Requesting WhatsApp OAuth2 access token", context);

        try {
            String tokenUrl = "https://graph.facebook.com/v18.0/oauth/access_token";
            
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credentials");
            params.put("client_id", configService.getWhatsappPhoneNumberId());
            params.put("client_secret", configService.getWhatsappAccessToken());
            params.put("scope", "whatsapp_business_messaging");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<OAuth2TokenResponse> response = restTemplate.postForEntity(
                tokenUrl, request, OAuth2TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                OAuth2TokenResponse tokenResponse = response.getBody();
                tokenResponse.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
                
                auditLogService.logOAuth2TokenRequest("whatsapp", "success", context);
                loggerUtility.logInfo("WhatsApp OAuth2 token obtained successfully", context);
                
                return tokenResponse;
            } else {
                auditLogService.logOAuth2TokenRequest("whatsapp", "failed", context);
                loggerUtility.logError("Failed to obtain WhatsApp OAuth2 token", context, 
                    new RuntimeException("HTTP " + response.getStatusCode()));
                return null;
            }

        } catch (Exception e) {
            auditLogService.logOAuth2TokenRequest("whatsapp", "error", context);
            loggerUtility.logError("Error obtaining WhatsApp OAuth2 token", context, e);
            return null;
        }
    }

    public OAuth2TokenResponse refreshWhatsAppAccessToken(String refreshToken) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("OAUTH2_WHATSAPP_REFRESH")
            .withMetadata("service", "whatsapp");
        
        loggerUtility.logInfo("Refreshing WhatsApp OAuth2 access token", context);

        try {
            String tokenUrl = "https://graph.facebook.com/v18.0/oauth/access_token";
            
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "refresh_token");
            params.put("refresh_token", refreshToken);
            params.put("client_id", configService.getWhatsappPhoneNumberId());
            params.put("client_secret", configService.getWhatsappAccessToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<OAuth2TokenResponse> response = restTemplate.postForEntity(
                tokenUrl, request, OAuth2TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                OAuth2TokenResponse tokenResponse = response.getBody();
                tokenResponse.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
                
                auditLogService.logOAuth2TokenRefresh("whatsapp", "success", context);
                loggerUtility.logInfo("WhatsApp OAuth2 token refreshed successfully", context);
                
                return tokenResponse;
            } else {
                auditLogService.logOAuth2TokenRefresh("whatsapp", "failed", context);
                loggerUtility.logError("Failed to refresh WhatsApp OAuth2 token", context, 
                    new RuntimeException("HTTP " + response.getStatusCode()));
                return null;
            }

        } catch (Exception e) {
            auditLogService.logOAuth2TokenRefresh("whatsapp", "error", context);
            loggerUtility.logError("Error refreshing WhatsApp OAuth2 token", context, e);
            return null;
        }
    }

    public boolean validateWhatsAppToken(String accessToken) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("OAUTH2_WHATSAPP_VALIDATE")
            .withMetadata("service", "whatsapp");
        
        loggerUtility.logInfo("Validating WhatsApp OAuth2 access token", context);

        try {
            String validateUrl = "https://graph.facebook.com/v18.0/me?access_token=" + accessToken;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.getForEntity(validateUrl, Map.class);

            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            
            auditLogService.logOAuth2TokenValidation("whatsapp", isValid ? "success" : "failed", context);
            loggerUtility.logInfo("WhatsApp OAuth2 token validation result: " + isValid, context);
            
            return isValid;

        } catch (Exception e) {
            auditLogService.logOAuth2TokenValidation("whatsapp", "error", context);
            loggerUtility.logError("Error validating WhatsApp OAuth2 token", context, e);
            return false;
        }
    }

    public OAuth2TokenResponse getInternalServiceToken(String serviceName, String[] scopes) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("OAUTH2_INTERNAL_TOKEN")
            .withMetadata("service", serviceName)
            .withMetadata("scopes", String.join(",", scopes));
        
        loggerUtility.logInfo("Requesting internal service OAuth2 token", context);

        try {
            // Internal service token generation (simplified)
            String token = generateInternalServiceToken(serviceName, scopes);
            
            OAuth2TokenResponse tokenResponse = new OAuth2TokenResponse();
            tokenResponse.setAccessToken(token);
            tokenResponse.setTokenType("Bearer");
            tokenResponse.setExpiresIn(3600); // 1 hour
            tokenResponse.setExpiresAt(LocalDateTime.now().plusSeconds(3600));
            tokenResponse.setScope(String.join(" ", scopes));
            
            auditLogService.logOAuth2TokenRequest(serviceName, "success", context);
            loggerUtility.logInfo("Internal service OAuth2 token generated successfully", context);
            
            return tokenResponse;

        } catch (Exception e) {
            auditLogService.logOAuth2TokenRequest(serviceName, "error", context);
            loggerUtility.logError("Error generating internal service OAuth2 token", context, e);
            return null;
        }
    }

    public boolean validateInternalServiceToken(String token, String requiredService) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("OAUTH2_INTERNAL_VALIDATE")
            .withMetadata("service", requiredService);
        
        loggerUtility.logInfo("Validating internal service OAuth2 token", context);

        try {
            // Internal service token validation (simplified)
            boolean isValid = validateInternalToken(token, requiredService);
            
            auditLogService.logOAuth2TokenValidation(requiredService, isValid ? "success" : "failed", context);
            loggerUtility.logInfo("Internal service OAuth2 token validation result: " + isValid, context);
            
            return isValid;

        } catch (Exception e) {
            auditLogService.logOAuth2TokenValidation(requiredService, "error", context);
            loggerUtility.logError("Error validating internal service OAuth2 token", context, e);
            return false;
        }
    }

    private String generateInternalServiceToken(String serviceName, String[] scopes) {
        // Simplified internal token generation
        // In real implementation, this would use proper OAuth2 flow
        return "internal_" + serviceName + "_" + System.currentTimeMillis();
    }

    private boolean validateInternalToken(String token, String requiredService) {
        // Simplified internal token validation
        // In real implementation, this would validate against internal OAuth2 server
        return token != null && token.startsWith("internal_" + requiredService + "_");
    }

    public static class OAuth2TokenResponse {
        private String accessToken;
        private String tokenType;
        private long expiresIn;
        private LocalDateTime expiresAt;
        private String refreshToken;
        private String scope;

        // Getters and Setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
