package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.AIAgentService;
import com.example.marketsupplier.service.CustomerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * n8n Workflow için özel endpoint'ler
 * WhatsApp Trigger → AI Node → Switch/IF Node → HTTP Request Node → WhatsApp Response Node
 */
@RestController
@RequestMapping("/api/n8n")
public class N8nWorkflowController {

    private static final Logger log = LoggerFactory.getLogger(N8nWorkflowController.class);
    
    @Autowired
    private AIAgentService aiAgentService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * n8n'den gelen WhatsApp mesajları için ana endpoint
     * Tamamen AI tabanlı, state ve context'e göre yanıt üretir
     */
    @PostMapping(value = "/whatsapp/message", 
                 consumes = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8",
                 produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, Object>> processWhatsAppMessage(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received n8n WhatsApp message payload: {}", payload);
            log.debug("Payload type: {}, size: {}", payload.getClass().getSimpleName(), payload.size());
            
            // n8n'den gelen veriyi parse et
            String from = extractValue(payload, "from", "phone", "phoneNumber");
            String text = extractValue(payload, "text", "message", "body");
            
            if (from == null || text == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing required fields: from and text"
                ));
            }
            
            // Telefon numarasını normalize et
            from = normalizePhoneNumber(from);
            
            // AI servisi ile işle - tamamen context-aware
            String response = aiAgentService.processMessage(from, text);
            
            // n8n'in beklediği format
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("to", from);
            result.put("type", "text");
            
            // Intent sınıflandırması için ekstra bilgi (n8n switch node için)
            result.put("intent_category", categorizeResponse(response));
            result.put("has_cart_action", response.contains("sepet") || response.contains("Sepet"));
            result.put("is_order_related", response.contains("sipariş") || response.contains("onayla"));
            
            log.info("Generated n8n response for {}: {}", from, response);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing n8n WhatsApp message", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Internal server error: " + e.getMessage(),
                "response", "Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin."
            ));
        }
    }

    /**
     * n8n Switch/IF Node için intent analizi endpoint'i
     * AI'nın belirlediği intent'e göre workflow yönlendirmesi
     */
    @PostMapping("/intent/analyze")
    public ResponseEntity<Map<String, Object>> analyzeIntent(@RequestBody Map<String, Object> payload) {
        try {
            String text = extractValue(payload, "text", "message");
            String from = extractValue(payload, "from", "phone");
            
            if (text == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "intent", "unknown"
                ));
            }
            
            // AI ile intent analizi (context ile birlikte)
            String response = aiAgentService.processMessage(from != null ? normalizePhoneNumber(from) : "unknown", text);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("intent", categorizeResponse(response));
            result.put("is_chat", response.contains("sohbet") || isConversationalResponse(response));
            result.put("is_ordering", response.contains("sepet") || response.contains("sipariş") || response.contains("ürün"));
            result.put("is_system", response.contains("mod") || response.contains("Mode"));
            result.put("needs_confirmation", response.contains("onayla") || response.contains("onaylıyor"));
            result.put("is_error", response.contains("hata") || response.contains("sorun"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error analyzing intent for n8n", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "intent", "error"
            ));
        }
    }

    /**
     * n8n için cart durumu endpoint'i
     */
    @GetMapping("/cart/status/{phone}")
    public ResponseEntity<Map<String, Object>> getCartStatus(@PathVariable String phone) {
        try {
            phone = normalizePhoneNumber(phone);
            
            // Cart durumunu kontrol et (cart işlemleri context ile AI tarafından yönetiliyor)
            String cartInfo = aiAgentService.processMessage(phone, "sepeti göster");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("has_cart", !cartInfo.contains("boş"));
            result.put("cart_info", cartInfo);
            result.put("is_empty", cartInfo.contains("boş") || cartInfo.contains("Sepetiniz boş"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting cart status for n8n", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * n8n için özel komutlar endpoint'i
     */
    @PostMapping("/command/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(@RequestBody Map<String, Object> payload) {
        try {
            String command = extractValue(payload, "command");
            String from = extractValue(payload, "from", "phone");
            
            if (command == null || from == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing command or phone"
                ));
            }
            
            from = normalizePhoneNumber(from);
            
            // Özel komutları AI ile işle
            String response = switch (command.toLowerCase()) {
                case "clear_cart" -> aiAgentService.processMessage(from, "sepeti boşalt");
                case "show_cart" -> aiAgentService.processMessage(from, "sepeti göster");
                case "show_products" -> aiAgentService.processMessage(from, "ürünleri göster");
                case "confirm_order" -> aiAgentService.processMessage(from, "siparişi onayla");
                case "show_modes" -> aiAgentService.processMessage(from, "mod");
                default -> aiAgentService.processMessage(from, command);
            };
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("command", command);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error executing command for n8n", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * n8n workflow health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "n8n-whatsapp-ai-assistant",
            "ai_enabled", true,
            "features", Map.of(
                "context_aware", true,
                "state_driven", true,
                "no_fixed_responses", true,
                "edge_case_handling", true
            )
        ));
    }

    // Helper methods
    private String extractValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        // Basit normalizasyon - gerçek implementasyon daha detaylı olabilir
        return phone.replaceAll("[^0-9+]", "");
    }

    private String categorizeResponse(String response) {
        if (response == null) return "unknown";
        
        // AI'nın ürettiği yanıta göre kategori belirle
        if (response.contains("mod") || response.contains("Mode")) return "mode_management";
        if (response.contains("sepet") && (response.contains("eklendi") || response.contains("çıkarıldı"))) return "cart_action";
        if (response.contains("sipariş") && response.contains("oluşturuldu")) return "order_created";
        if (response.contains("onayla") || response.contains("onaylıyor")) return "confirmation_needed";
        if (response.contains("boş") && response.contains("sepet")) return "empty_cart";
        if (response.contains("ürün") || response.contains("Mevcut")) return "product_listing";
        if (response.contains("hata") || response.contains("sorun")) return "error";
        
        return "chat";
    }

    private boolean isConversationalResponse(String response) {
        if (response == null) return false;
        String lower = response.toLowerCase();
        return lower.contains("merhaba") || 
               lower.contains("nasıl") || 
               lower.contains("yardım") ||
               lower.contains("sohbet") ||
               (!lower.contains("sepet") && !lower.contains("sipariş") && !lower.contains("ürün"));
    }
}
