package com.example.marketsupplier.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.marketsupplier.service.WhatsAppService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.OrderService;
import com.example.marketsupplier.service.ProductService;
import com.example.marketsupplier.service.EnhancedAIService;
import com.example.marketsupplier.service.AIAgentService;
import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.service.WhatsAppRoutingService;
import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import com.example.marketsupplier.util.InputSanitizer;
import com.example.marketsupplier.util.InputValidator;
import com.example.marketsupplier.util.LoggerUtility;
import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.service.ApplicationMetricsService;
import com.example.marketsupplier.service.RateLimitService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;
import com.example.marketsupplier.service.DeliveryService;
import org.springframework.context.ApplicationContext;
import com.example.marketsupplier.agent.dedup.DedupService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Locale;

@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${app.whatsapp.verify_token:}")
    private String verifyToken;

    private final WhatsAppService whatsAppService;
    private final ApplicationContext applicationContext;
    private final AIAgentService aiAgentService;
    private final DedupService dedupService;
    private final InputSanitizer inputSanitizer;
    private final InputValidator inputValidator;
    private final LoggerUtility loggerUtility;
    private final ConfigService configService;
    private final ApplicationMetricsService metricsService;
    private final RateLimitService rateLimitService;
    private final WhatsAppRoutingService routingService;

    // Mod mantığı kaldırıldı; tüm mesajlar doğrudan AI servisine yönlendirilir

    @Autowired
    public WhatsAppWebhookController(WhatsAppService whatsAppService,
                                   ApplicationContext applicationContext,
                                   AIAgentService aiAgentService,
                                   DedupService dedupService,
                                   InputSanitizer inputSanitizer,
                                   InputValidator inputValidator,
                                   LoggerUtility loggerUtility,
                                   ConfigService configService,
                                   ApplicationMetricsService metricsService,
                                   RateLimitService rateLimitService,
                                   WhatsAppRoutingService routingService) {
        this.whatsAppService = whatsAppService;
        this.applicationContext = applicationContext;
        this.aiAgentService = aiAgentService;
        this.dedupService = dedupService;
        this.inputSanitizer = inputSanitizer;
        this.inputValidator = inputValidator;
        this.loggerUtility = loggerUtility;
        this.configService = configService;
        this.metricsService = metricsService;
        this.rateLimitService = rateLimitService;
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
                                                @RequestParam("hub.challenge") String challenge,
                                                @RequestParam("hub.verify_token") String token) {
        log.info("WhatsApp webhook verification request received. Mode: {}, Token provided: {}, Challenge: {}", mode, token, challenge);
        if ("subscribe".equalsIgnoreCase(mode) && verifyToken.equals(token)) {
            log.info("Webhook verification successful. Responding with challenge.");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Webhook verification failed. Mode: {} or token mismatch.", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed. Token mismatch.");
        }
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        long startTime = System.currentTimeMillis();
        String messageType = "unknown";
        String status = "success";

        try {
            // Check if WhatsApp is enabled
            if (!configService.isWhatsappEnabled()) {
                loggerUtility.logWarn("WhatsApp is disabled", LoggerUtility.LogContext.create("WEBHOOK_RECEIVE"));
                metricsService.recordWhatsAppMessage("disabled", "disabled", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            LoggerUtility.LogContext context = LoggerUtility.LogContext.create("WEBHOOK_RECEIVE")
                .withMetadata("payload_length", payload != null ? payload.length() : 0)
                .withMetadata("whatsapp_enabled", configService.isWhatsappEnabled())
                .withMetadata("rate_limiting_enabled", configService.isRateLimitingEnabled());
            
            loggerUtility.logInfo("WhatsApp webhook received", context);

            // Input sanitization
            String sanitizedPayload = inputSanitizer.sanitizeText(payload);
            if (sanitizedPayload == null) {
                loggerUtility.logSecurity("Payload sanitization failed", context, Map.of(
                    "payload", payload,
                    "security_threat", "potential_xss_or_injection"
                ));
                metricsService.recordError("payload_sanitization", "WhatsAppWebhookController");
                metricsService.recordWhatsAppMessage("sanitization_error", "error", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // HTML entity'leri decode et
            String decodedPayload = decodeHtmlEntities(sanitizedPayload);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(decodedPayload);

            JsonNode entry = root.path("entry");
            if (!entry.isArray() || entry.isEmpty()) {
                return ResponseEntity.ok().build();
            }
            JsonNode changes = entry.get(0).path("changes");
            if (!changes.isArray() || changes.isEmpty()) {
                return ResponseEntity.ok().build();
            }
            JsonNode value = changes.get(0).path("value");
            JsonNode messages = value.path("messages");
            
            // Extract phone_number_id for routing
            String phoneNumberId = value.path("metadata").path("phone_number_id").asText("");
            if (phoneNumberId.isEmpty()) {
                loggerUtility.logWarn("No phone_number_id found in webhook", context);
                return ResponseEntity.ok().build();
            }

            if (!messages.isArray() || messages.isEmpty()) {
                // Bu bir mesaj değil, durum güncellemesi olabilir.
                return ResponseEntity.ok().build();
            }

            JsonNode msg = messages.get(0);
            String from = msg.path("from").asText("");
            String type = msg.path("type").asText("");
            
            // Route to correct supplier based on phone_number_id
            com.example.marketsupplier.entity.Supplier supplier = routingService.routeToSupplier(phoneNumberId, from);
            if (supplier == null) {
                loggerUtility.logWarn("No supplier found for phone_number_id: " + phoneNumberId, context);
                return ResponseEntity.ok().build();
            }
            
            loggerUtility.logInfo("Successfully routed to supplier: " + supplier.getCompanyName(), context);

            // Comprehensive input validation
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("from", from);
            messageData.put("type", type);
            
            // Get text for validation
            String text = msg.path("text").path("body").asText("");
            messageData.put("text", text);
            
            Map<String, InputValidator.ValidationResult> validationResults = inputValidator.validateWhatsAppMessage(messageData);
            
            if (!inputValidator.isAllValid(validationResults)) {
                List<String> errors = inputValidator.getAllErrors(validationResults);
                log.warn("Input validation failed: {}", errors);
                metricsService.recordValidationError("whatsapp_message", "validation_failed");
                metricsService.recordWhatsAppMessage("validation_error", "error", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Use sanitized values
            from = inputValidator.getSanitizedValue(validationResults, "from");
            type = inputValidator.getSanitizedValue(validationResults, "type");

            if (!"text".equals(type)) {
                messageType = "non_text";
                if (!from.isBlank()) {
                    whatsAppService.sendTextMessage(from, "Şimdilik sadece metin mesajlarını anlayabiliyorum. Yakında ses/görsel desteği eklenecek.");
                }
                metricsService.recordWhatsAppMessage(messageType, "success", System.currentTimeMillis() - startTime);
                return ResponseEntity.ok().build();
            }

            // Use sanitized text from validation
            text = inputValidator.getSanitizedValue(validationResults, "text");
            
            // Additional text validation if needed
            InputValidator.ValidationResult textValidation = inputValidator.validateMessage(text);
            if (!textValidation.isValid()) {
                log.warn("Message validation failed for user {}: {}", from, textValidation.getErrors());
                metricsService.recordValidationError("message_text", "validation_failed");
                if (!from.isBlank()) {
                    whatsAppService.sendTextMessage(from, "Mesajınız güvenlik nedeniyle işlenemedi. Lütfen farklı bir mesaj gönderin.");
                }
                metricsService.recordWhatsAppMessage("text_validation_error", "error", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Use sanitized text
            text = textValidation.getSanitizedValue();
            
            if (text == null || text.trim().isEmpty()) {
                messageType = "empty_text";
                if (!from.isBlank()) {
                    whatsAppService.sendTextMessage(from, "Boş bir mesaj aldım, nasıl yardımcı olabilirim?");
                }
                metricsService.recordWhatsAppMessage(messageType, "success", System.currentTimeMillis() - startTime);
                return ResponseEntity.ok().build();
            }

            // Rate limiting check
            if (!rateLimitService.isAllowed(from)) {
                log.warn("Rate limit exceeded for user: {}", from);
                metricsService.recordRateLimitExceeded(from, "webhook");
                whatsAppService.sendTextMessage(from, "Çok fazla mesaj gönderiyorsunuz. Lütfen bir dakika bekleyin.");
                metricsService.recordWhatsAppMessage("rate_limit_exceeded", "error", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }

            log.info("Incoming from:{} type:{} text:{}", from, type, text);

            // Tüm işlemleri AIAgentService'e devret
            try {
                // Duplicate webhook koruması
                String messageId = findMessageId(msg);
                if (messageId != null && dedupService.isDuplicate(messageId)) {
                    log.info("Duplicate webhook ignored: {}", messageId);
                    metricsService.recordWhatsAppMessage("duplicate", "success", System.currentTimeMillis() - startTime);
                    return ResponseEntity.ok().build();
                }

                log.info("Forwarding message from {} to AIAgentService", from);
                String aiResponse = aiAgentService.processMessage(from, text);
                
                // AI cevabını kontrol et
                if (aiResponse == null || aiResponse.trim().isEmpty()) {
                    log.warn("AI returned empty response for user {}", from);
                    aiResponse = "Üzgünüm, şu anda size yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.";
                    metricsService.recordAIAgentFallback("empty_response");
                }
                
                log.info("AI response for user {}: {}", from, aiResponse);
                whatsAppService.sendTextMessage(from, aiResponse);
                
                // Record success metrics
                messageType = "text";
                metricsService.recordValidationSuccess("message_text");
                metricsService.recordRateLimitAllowed(from, "webhook");
                metricsService.recordWhatsAppMessage(messageType, status, System.currentTimeMillis() - startTime);
                
                return ResponseEntity.ok().build();
                    
            } catch (Exception e) {
                status = "error";
                log.error("Error processing message with AIAgentService for user {}", from, e);
                metricsService.recordError("whatsapp_processing", "WhatsAppWebhookController");
                whatsAppService.sendTextMessage(from, "Üzgünüm, şu anda size yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.");
                metricsService.recordWhatsAppMessage(messageType, status, System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        } catch (Exception e) {
            status = "error";
            log.error("Error processing webhook payload", e);
            metricsService.recordError("webhook_processing", "WhatsAppWebhookController");
            metricsService.recordWhatsAppMessage("payload_error", status, System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // Menü/sohbet modu kaldırıldı

    private String findMessageId(JsonNode msg) {
        if (msg != null && msg.has("id")) {
            return msg.get("id").asText(null);
        }
        return null;
    }

    private String decodeHtmlEntities(String input) {
        if (input == null) {
            return null;
        }
        
        return input
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'");
    }
}


