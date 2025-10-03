package com.example.marketsupplier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EnhancedAIService {
    
    private static final Logger log = LoggerFactory.getLogger(EnhancedAIService.class);
    
    @Value("${app.ai.agent.url:http://localhost:8000}")
    private String aiAgentUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public EnhancedAIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Gelişmiş AI agent'a mesaj gönder ve yanıt al
     */
    public String processMessage(String message, String phone) {
        try {
            // Request body oluştur
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", message);
            requestBody.put("phone", phone);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // AI Agent'a POST request gönder - ResponseEntity<Map> kullan
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiAgentUrl + "/parse-order", 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> aiResponse = response.getBody();
                log.info("AI Agent response: " + aiResponse);
                
                // AI Agent'ın response'unda 'message' field'ı var
                String responseMessage = (String) aiResponse.get("message");
                if (responseMessage != null && !responseMessage.isEmpty()) {
                    log.info("Using message field: " + responseMessage);
                    return responseMessage;
                }
                
                // Fallback olarak confirmation_message'ı da dene
                String confirmationMessage = (String) aiResponse.get("confirmation_message");
                if (confirmationMessage != null && !confirmationMessage.isEmpty()) {
                    log.info("Using confirmation_message field: " + confirmationMessage);
                    return confirmationMessage;
                }
                
                log.warn("No valid message found in AI response: " + aiResponse);
                return "Size nasıl yardımcı olabilirim?";
            }
            
            log.warn("AI Agent returned non-OK status: " + response.getStatusCode());
            return "Üzgünüm, şu anda size yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.";
            
        } catch (Exception e) {
            log.error("Enhanced AI Agent çağrısında hata: " + e.getMessage(), e);
            return "Üzgünüm, şu anda size yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.";
        }
    }
    
    /**
     * AI agent'ın sipariş işleme sonucunu al
     */
    public Map<String, Object> processOrder(String message, String phone) {
        try {
            // Request body oluştur
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", message);
            requestBody.put("phone", phone);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");
            
            // Request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // AI Agent'a POST request gönder - ResponseEntity<Map> kullan
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiAgentUrl + "/parse-order", 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // AI Agent'ın response'unda 'message' field'ı var
                Map<String, Object> aiResponse = response.getBody();
                log.info("AI Agent processOrder response: " + aiResponse);
                return aiResponse;
            }
            
            return new HashMap<>();
            
        } catch (Exception e) {
            log.error("Enhanced AI Agent sipariş işleme hatası: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Sipariş onaylama
     */
    public String confirmOrder(String phone, String confirmation) {
        try {
            // Request body oluştur
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phone);
            requestBody.put("confirmation", confirmation);
            requestBody.put("pending_items", new Object[]{}); // Boş array
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // AI Agent'a POST request gönder
            String response = restTemplate.postForObject(
                aiAgentUrl + "/confirm-order", 
                request, 
                String.class
            );
            
            if (response != null) {
                // JSON response'u parse et
                Map<String, Object> aiResponse = objectMapper.readValue(response, Map.class);
                // AI Agent'ın response'unda 'message' field'ı var, 'confirmation_message' değil
                String message = (String) aiResponse.get("message");
                if (message != null && !message.isEmpty()) {
                    return message;
                }
                // Fallback olarak confirmation_message'ı da dene
                return (String) aiResponse.getOrDefault("confirmation_message", "Size nasıl yardımcı olabilirim?");
            }
            
            return "Sipariş işleme hatası oluştu.";
            
        } catch (Exception e) {
            log.error("Enhanced AI Agent sipariş onaylama hatası: " + e.getMessage(), e);
            return "Sipariş işleme hatası oluştu.";
        }
    }
}
