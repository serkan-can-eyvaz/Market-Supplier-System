package com.example.marketsupplier.service;

import com.example.marketsupplier.agent.AgentOrchestrator;
import com.example.marketsupplier.agent.context.ContextManager;
import com.example.marketsupplier.agent.OrchestrationResult;
import com.example.marketsupplier.config.ConfigService;
import com.example.marketsupplier.util.InputValidator;
import com.example.marketsupplier.util.LoggerUtility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.marketsupplier.agent.nlp.LLMFallbackHandler;
import com.example.marketsupplier.service.CustomerContext;
import com.example.marketsupplier.entity.Cart;
import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderStatus;
import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.repository.CartItemRepository;
import com.example.marketsupplier.repository.CartRepository;
import com.example.marketsupplier.repository.OrderRepository;
import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.ProductService;
import com.example.marketsupplier.service.WhatsAppService;
import java.util.HashMap;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class AIAgentService {

    private static final Logger log = LoggerFactory.getLogger(AIAgentService.class);
    private final CartService cartService;
    private final OrderService orderService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MarketService marketService;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CustomerContext> localContextCache = new ConcurrentHashMap<>();
    
    @Autowired
    @Lazy
    private AgentOrchestrator agentOrchestrator;

    @Autowired
    private ContextManager contextManager;

    @Autowired(required = false)
    private LLMFallbackHandler llmFallbackHandler;

    @Autowired
    private CriticalServiceWrapper criticalServiceWrapper;

    @Autowired
    private InputValidator inputValidator;
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    @Lazy
    private ProductService productService;
    
    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private LoggerUtility loggerUtility;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AIAgentService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private ApplicationMetricsService metricsService;

    public AIAgentService(
            CartService cartService,
            OrderService orderService,
            RedisTemplate<String, Object> redisTemplate,
            MarketService marketService,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${ai.api.base_url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.model:gpt-4o-mini}") String model
    ) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
        this.marketService = marketService;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        
        // Debug logging
        log.info("AIAgentService initialized with:");
        log.info("- API Key: {}", apiKey != null && !apiKey.isEmpty() ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "EMPTY");
        log.info("- Base URL: {}", baseUrl);
        log.info("- Model: {}", model);
        
        // Validate API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("CRITICAL: OpenAI API key is not configured! AI features will not work.");
        } else if (!apiKey.startsWith("sk-")) {
            log.warn("WARNING: API key format may be incorrect. Expected format: sk-...");
        } else {
            log.info("API key format validation passed.");
        }
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    private CustomerContext getContext(String phone) {
        com.example.marketsupplier.agent.context.ContextManager.CartContext cartCtx = contextManager.getContext(phone);
        if (cartCtx != null) {
            CustomerContext ctx = new CustomerContext(phone);
            ctx.setSessionData(cartCtx.getMetadata());
            updateCartContext(phone, ctx);
            return ctx;
        }
        return localContextCache.get(phone);
    }

    private void saveContext(String phone, CustomerContext ctx) {
        com.example.marketsupplier.agent.context.ContextManager.CartContext cartCtx = 
            new com.example.marketsupplier.agent.context.ContextManager.CartContext();
        cartCtx.setMetadata(ctx.getSessionData());
        contextManager.saveContext(phone, cartCtx);
        localContextCache.remove(phone);
    }

    public String processMessage(String from, String text) {
        long startTime = System.currentTimeMillis();
        String intent = "unknown";
        String status = "success";

        try {
            if (!configService.isAiAgentEnabled()) {
                loggerUtility.logWarn("AI agent is disabled", LoggerUtility.LogContext.create("PROCESS_MESSAGE", from));
                metricsService.recordAIAgentRequest("disabled", "disabled", System.currentTimeMillis() - startTime);
                return "AI servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin.";
            }

            LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PROCESS_MESSAGE", from)
                .withMetadata("message_length", text != null ? text.length() : 0)
                .withMetadata("ai_provider", configService.getAiProvider())
                .withMetadata("ai_model", configService.getAiModel());
            
            loggerUtility.logInfo("Processing incoming message", context);
        
            InputValidator.ValidationResult phoneValidation = inputValidator.validatePhoneNumber(from);
            if (!phoneValidation.isValid()) {
                // Handle invalid phone...
                return "Geçersiz telefon numarası formatı.";
            }
            
            InputValidator.ValidationResult textValidation = inputValidator.validateMessage(text);
            if (!textValidation.isValid()) {
                // Handle invalid message...
                return "Mesajınız güvenlik nedeniyle işlenemedi. Lütfen farklı bir mesaj gönderin.";
            }
        
            final String finalFrom = phoneValidation.getSanitizedValue();
            final String finalText = textValidation.getSanitizedValue();
            context = context.withMetadata("sanitized_message_length", finalText.length());
            
            CustomerContext ctx = getContext(finalFrom);
            if (ctx == null) {
                ctx = new CustomerContext(finalFrom);
                loggerUtility.logInfo("Created new customer context", context);
            }
            final CustomerContext finalCtx = ctx;
            ctx.addConversationLine("USER: " + finalText);

            updateCartContext(finalFrom, ctx);
            log.info("Initial cart context set - hasActiveCart: {}, state: {}", ctx.isHasActiveCart(), ctx.getLastCartState());

            long orchestrationStartTime = System.currentTimeMillis();
            OrchestrationResult orchestrationResult = agentOrchestrator.handle(finalFrom, finalText, finalCtx);
            long orchestrationTime = System.currentTimeMillis() - orchestrationStartTime;
            
            loggerUtility.logPerformance("AGENT_ORCHESTRATION", orchestrationTime, context);

            String finalResponse;
            String actionResult = orchestrationResult.getResponse();
            intent = orchestrationResult.getIntent();
            boolean isProductList = orchestrationResult.isProductList();

            if ("add_to_cart".equals(intent) || "remove_from_cart".equals(intent) || "update_cart".equals(intent) || "clear_cart".equals(intent)) {
                finalResponse = handleViewCart(finalFrom);
            } else if ("rate_limit".equals(intent) && "RATE_LIMIT_FALLBACK".equals(actionResult)) {
                // Rate limit durumunda fallback yanıt ver
                finalResponse = generateRateLimitFallbackResponse(finalText, finalCtx);
            } else if ("chat".equals(intent) && "AI_CONTEXTUAL_RESPONSE".equals(actionResult)) {
                // Chat için AI-driven contextual yanıt üret, başarısız olursa fallback
                try {
                long llmStartTime = System.currentTimeMillis();
                String llm = criticalServiceWrapper.executeLlmOperationWithRetry(() -> 
                        generateContextualChatResponse(finalCtx, finalText)
                    );
                long llmTime = System.currentTimeMillis() - llmStartTime;
                    loggerUtility.logPerformance("LLM_CONTEXTUAL_CHAT", llmTime, context);
                    finalResponse = (llm == null || llm.isBlank()) ? generateFallbackChatResponse(finalText, finalCtx) : llm;
                } catch (Exception e) {
                    log.warn("AI chat response failed, using fallback: {}", e.getMessage());
                    finalResponse = generateFallbackChatResponse(finalText, finalCtx);
                }
            } else if (isProductList) {
                    log.info("Product list detected, returning directly without AI processing");
                    finalResponse = actionResult;
                } else {
                    long llmStartTime = System.currentTimeMillis();
                    String llm = criticalServiceWrapper.executeLlmOperationWithRetry(() ->
                        generateAiActionAwareReply(finalCtx, finalText, actionResult)
                    );
                    long llmTime = System.currentTimeMillis() - llmStartTime;
                    loggerUtility.logPerformance("LLM_ACTION_GENERATION", llmTime, context);
                    finalResponse = (llm == null || llm.isBlank()) ? actionResult : llm;
            }

            ctx.addConversationLine("ASSISTANT: " + finalResponse);
            saveContext(from, ctx);
            
            // Final logging and metrics...
            return finalResponse;
            
        } catch (Exception e) {
            // Error handling...
            return "Üzgünüm, mesajınızı işlerken bir hata oluştu. Lütfen daha sonra tekrar deneyin.";
        }
    }

    // Sohbet akışını tamamen LLM yöneteceği için bu yardımcılar artık kullanılmıyor
    private boolean isQuantityOnly(String message) { return false; }
    private boolean looksLikeProductQuery(String message) { return false; }
    private boolean isGreeting(String message) { return false; }

    private String findLastUserHint(CustomerContext ctx) {
        // Son kullanıcı mesajları içinde sayısal olmayan en yakın ipucunu bul
        List<String> hist = ctx.getConversationHistory();
        for (int i = hist.size() - 2; i >= 0; i--) { // son satır mevcut mesajdır, bir önceklere bak
            String line = hist.get(i);
            if (line.startsWith("USER:")) {
                String t = line.substring(5).trim();
                if (!isQuantityOnly(t)) {
                    return t;
                }
            }
        }
        return null;
    }

    private String buildCatalogOptions(String query) {
        try {
            // ProductService circular dependency'yi önlemek için kaldırıldı
            List<com.example.marketsupplier.entity.Product> products = new ArrayList<>();
            if (products == null || products.isEmpty()) return "Şu anda aktif ürün bulunmuyor.";
            String q = query.toLowerCase(Locale.ROOT);
            List<com.example.marketsupplier.entity.Product> matches = new ArrayList<>();
            for (com.example.marketsupplier.entity.Product p : products) {
                String name = p.getName() != null ? p.getName().toLowerCase(Locale.ROOT) : "";
                if (name.contains(q)) {
                    matches.add(p);
                }
            }
            if (matches.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("Mevcut seçenekler (katalogtan):\n");
            matches.stream().limit(10).forEach(p -> sb.append("- ")
                    .append(p.getName())
                    .append(" (")
                    .append(p.getUnit())
                    .append(") - ")
                    .append(p.getPrice()).append(" TL\n"));
            if (matches.size() > 10) sb.append("… ve daha fazlası");
            sb.append("\nÖrn: '2 ").append(matches.get(0).getUnit()).append(" ")
                    .append(matches.get(0).getName()).append("' şeklinde yazabilirsiniz.");
            return sb.toString();
        } catch (Exception e) {
            log.warn("buildCatalogOptions failed", e);
            return "";
        }
    }

    private String classifyIntent(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("sepet") && (lower.contains("göster") || lower.contains("görüntüle") || lower.contains("ne var"))) return "view_cart";
        if (lower.contains("sepete") || lower.contains("ekle") || lower.matches(".*(\\d+).*(koli|kg|adet).+")) return "add_to_cart";
        if (lower.contains("onayla") || lower.contains("tamam") || lower.contains("onaylıyorum")) return "create_order";
        if (lower.contains("sipariş") && (lower.contains("ver") || lower.contains("oluştur"))) return "create_order";
        if (lower.contains("ürün") || lower.contains("var mı") || lower.matches(".*(elma|armut|domates|patates).*")) return "get_products";
        return "chat";
    }

    private void updateCartContext(String phone, CustomerContext ctx) {
        try {
            marketService.findByPhoneNormalized(phone).ifPresent(market -> {
                log.info("Updating cart context for phone: {}, marketId: {}", phone, market.getId());
                
                // Cart timeout kontrolü (2-3 saat)
                checkAndClearExpiredCart(market.getId(), phone);
                
                List<com.example.marketsupplier.entity.CartItem> cartItems = cartService.getItems(market.getId());
                log.info("Cart items count: {} for market: {}", cartItems.size(), market.getId());

                if (!cartItems.isEmpty()) {
                    ctx.setHasActiveCart(true);
                    
                    StringBuilder cartState = new StringBuilder("Mevcut sepet içeriği:\n");
                    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                    for (com.example.marketsupplier.entity.CartItem item : cartItems) {
                        cartState.append("- ").append(item.getProductName())
                                .append(" x").append(item.getQuantity())
                                .append(" ").append(item.getUnit())
                                .append(" (").append(item.getPrice()).append(" TL)\n");
                        total = total.add(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                    }
                    cartState.append(String.format(java.util.Locale.forLanguageTag("tr-TR"), "Toplam: %.2f TL", total));
                    ctx.setLastCartState(cartState.toString());
                    log.info("Cart context updated - hasActiveCart: true, state: {}", ctx.getLastCartState());
                } else {
                    ctx.setHasActiveCart(false);
                    ctx.setLastCartState("Sepet boş.");
                    log.info("Cart context updated - hasActiveCart: false, empty cart");
                }
            });
        } catch (Exception e) {
            log.error("Failed to update cart context for phone {}", phone, e);
            // Fallback: boş sepet olarak işaretle
            ctx.setHasActiveCart(false);
            ctx.setLastCartState("Sepet boş.");
        }
    }
    
    private void checkAndClearExpiredCart(Long marketId, String phone) {
        try {
            // Cart'ın son güncelleme zamanını kontrol et
            Cart cart = cartRepository.findLatestByMarketId(marketId).orElse(null);
            if (cart != null && cart.getUpdatedAt() != null) {
                LocalDateTime lastUpdate = cart.getUpdatedAt();
                LocalDateTime now = LocalDateTime.now();
                Duration timeDiff = Duration.between(lastUpdate, now);
                
                // 2 saat 30 dakika (150 dakika) geçmişse sepeti temizle
                if (timeDiff.toMinutes() > 150) {
                    log.info("Cart expired for marketId: {}, lastUpdate: {}, clearing cart", marketId, lastUpdate);
                    cartService.clearCart(marketId);
                    
                    // Müşteriye bildirim gönder
                    try {
                        String message = "Sepetiniz 2.5 saat boyunca güncellenmediği için temizlendi. Yeni sipariş vermek için ürün ekleyebilirsiniz.";
                        whatsAppService.sendTextMessage(phone, message);
                    } catch (Exception e) {
                        log.warn("Could not send cart timeout notification to {}", phone, e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking cart timeout for marketId: {}", marketId, e);
        }
    }
    
    private void updateCartContextAfterAction(String phone, CustomerContext ctx) {
        // Agresif context refresh - cache'i temizle ve database'den fresh oku
        try {
            log.info("Forcing cart context refresh after action");
            
            // Local cache'i temizle
            contextManager.clearLocalCache(phone);
            
            // Database'den fresh cart state al
            updateCartContext(phone, ctx);
            
            log.info("Force-refreshed cart context: hasActiveCart={}, cartState={}", 
                ctx.isHasActiveCart(), ctx.getLastCartState());
                
        } catch (Exception e) {
            log.warn("Error in force refresh", e);
            // Fallback - normal update
            updateCartContext(phone, ctx);
        }
    }

    private String generateContextualAiReply(CustomerContext ctx, String text, String systemSignal) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. Cannot generate contextual AI reply.");
            return handleSystemSignalFallback(systemSignal);
        }
        
        try {
            String apiUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";
            List<Map<String, String>> messages = new ArrayList<>();
            
            // Sistem promptu - contextual ve state-aware
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("You are MarketAsist, an intelligent WhatsApp AI ASSISTANT (NOT a bot) for grocery ordering.\n");
            systemPrompt.append("You are a helpful, friendly HUMAN-LIKE assistant who provides personalized service.\n");
            systemPrompt.append("Generate natural, contextual responses based on CURRENT STATE and user intent.\n");
            systemPrompt.append("Always speak Turkish naturally. Be professional, helpful, and conversational.\n");
            systemPrompt.append("CRITICAL: You are NOT a rule-based bot - understand context and nuance.\n");
            systemPrompt.append("CRITICAL: Always check cart state and respond accordingly.\n\n");
            
            // System signal'a göre özel talimatlar
            systemPrompt.append("CURRENT SITUATION: ");
            switch (systemSignal) {
                case "SHOW_MODES_REQUEST":
                    systemPrompt.append("User asked about available modes. Explain ordering mode vs chat mode naturally.\n");
                    break;
                case "INTENT_CHAT_MODE_RESTRICTED":
                    systemPrompt.append("User tried to perform ordering action in chat mode. Explain restriction naturally.\n");
                    break;
                case "INTENT_CHAT":
                    systemPrompt.append("User wants to chat. Respond naturally and conversationally.\n");
                    break;
                case "INTENT_GET_ORDER_STATUS":
                    systemPrompt.append("User asked about order status. The system will provide intelligent filtering:\n");
                    systemPrompt.append("- If there's an approved but not delivered order → show only that with delivery date\n");
                    systemPrompt.append("- If there are pending orders → show only last 5 pending orders\n");
                    systemPrompt.append("- If there are delivered orders → show only the last delivered order\n");
                    systemPrompt.append("Respond naturally to the order status information provided by the system.\n");
                    break;
                case "INTENT_SHOW_PRODUCT_CATEGORY":
                    systemPrompt.append("User asked for a product category (like 'kola', 'gazoz', 'içecek').\n");
                    systemPrompt.append("The system will show ALL products in that category from the database.\n");
                    systemPrompt.append("Respond naturally to the category products list provided by the system.\n");
                    systemPrompt.append("Encourage user to select the exact product name they want.\n");
                    break;
                case "RATE_LIMIT_EXCEEDED":
                    systemPrompt.append("System is experiencing high load. Apologize naturally and suggest trying again soon. Be helpful and understanding.\n");
                    break;
                default:
                    if (systemSignal.startsWith("MODE_CHANGED_")) {
                        String mode = systemSignal.substring("MODE_CHANGED_".length());
                        systemPrompt.append("User switched to ").append(mode).append(" mode. Confirm the change naturally.\n");
                    } else {
                        systemPrompt.append("General conversation.\n");
                    }
            }
            
            // Mevcut sepet durumu - çok açık şekilde belirt
            systemPrompt.append("\n--- CURRENT CART STATE ---\n");
            if (ctx.isHasActiveCart() && ctx.getLastCartState() != null && 
                !ctx.getLastCartState().trim().isEmpty() && 
                !ctx.getLastCartState().toLowerCase().contains("boş") &&
                !ctx.getLastCartState().toLowerCase().contains("empty")) {
                systemPrompt.append("CART HAS ITEMS:\n").append(ctx.getLastCartState()).append("\n");
            } else {
                systemPrompt.append("CART IS EMPTY: No items in cart, total is 0.00 TL\n");
            }
            systemPrompt.append("--- END CART STATE ---\n\n");
            
            messages.add(Map.of("role", "system", "content", systemPrompt.toString()));
            
            // Son birkaç konuşmayı ekle
            ctx.getConversationHistory().stream()
                    .skip(Math.max(0, ctx.getConversationHistory().size() - 4))
                    .forEach(line -> {
                        if (line.startsWith("USER:")) {
                            messages.add(Map.of("role", "user", "content", line.substring(5).trim()));
                        } else if (line.startsWith("ASSISTANT:")) {
                            messages.add(Map.of("role", "assistant", "content", line.substring(10).trim()));
                        }
                    });

            Map<String, Object> payload = Map.of(
                    "model", this.model,
                    "messages", messages,
                    "temperature", 0.6, // Biraz daha yaratıcı
                    "max_tokens", 250
            );
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(configService.getAiTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                try { Thread.sleep(500L); } catch (InterruptedException ignored) {}
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                log.error("LLM API request failed with status code {}: {}", response.statusCode(), response.body());
                return handleSystemSignalFallback(systemSignal);
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode choices = rootNode.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String responseText = choices.get(0).path("message").path("content").asText();
                if (responseText != null && !responseText.trim().isEmpty()) {
                    return responseText;
                }
            }
            
            return handleSystemSignalFallback(systemSignal);
            
        } catch (Exception e) {
            log.error("Error generating contextual AI reply", e);
            return handleSystemSignalFallback(systemSignal);
        }
    }
    
    private String handleSystemSignalFallback(String systemSignal) {
        // Minimal fallback - AI mevcut değilse temel yanıtlar
        switch (systemSignal) {
            case "SHOW_MODES_REQUEST":
                return "İki mod arasından seçim yapabilirsiniz:\n1) Sipariş modu - ürün ekleme ve sipariş oluşturma\n2) Sohbet modu - genel konuşma";
            case "INTENT_CHAT_MODE_RESTRICTED":
                return "Şu anda sohbet modundasınız. Sipariş işlemleri için 'mod' yazıp sipariş moduna geçebilirsiniz.";
            default:
                if (systemSignal.startsWith("MODE_CHANGED_")) {
                    String mode = systemSignal.substring("MODE_CHANGED_".length());
                    return "chat".equals(mode) ? "Sohbet moduna geçtiniz." : "Sipariş moduna geçtiniz.";
                }
                return "Merhaba! Size nasıl yardımcı olabilirim?";
        }
    }

    private String generateAiReplyWithFallback(CustomerContext ctx, String text) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String result = generateAiReply(ctx, text);
                if (result != null && !result.trim().isEmpty()) {
                    return result;
                }
                
                if (llmFallbackHandler != null && llmFallbackHandler.shouldRetry("llm_empty_response", attempt)) {
                    long delay = llmFallbackHandler.getRetryDelay(attempt);
                    Thread.sleep(delay);
                    continue;
                }
                break;
            } catch (Exception e) {
                log.warn("LLM attempt {} failed", attempt + 1, e);
                if (llmFallbackHandler != null && llmFallbackHandler.shouldRetry("llm_api_error", attempt)) {
                    try {
                        long delay = llmFallbackHandler.getRetryDelay(attempt);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        
        return llmFallbackHandler != null ? 
            llmFallbackHandler.getFallbackMessage("llm_empty_response") : 
            "Üzgünüm, bir sorun oluştu. Lütfen tekrar deneyin.";
    }

    private String generateAiReply(CustomerContext ctx, String text) {
        log.debug("Generating conversational AI reply for text: {}", text);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. Cannot generate AI reply.");
            return "Üzgünüm, bir sorun oluştu. Lütfen daha sonra tekrar deneyin.";
        }
        
        try {
            String apiUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";

            List<Map<String, String>> messages = new ArrayList<>();
            
            // --- AI AGENT SİSTEM PROMPTU ---
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("You are MarketAsist, an intelligent WhatsApp AI ASSISTANT (NOT a bot) for grocery ordering.\n");
            systemPrompt.append("You are a helpful, friendly HUMAN-LIKE assistant who provides personalized service.\n");
            systemPrompt.append("You understand natural language perfectly and provide contextual, state-aware responses.\n");
            systemPrompt.append("Always speak Turkish naturally. Be professional, helpful, and conversational.\n");
            systemPrompt.append("CRITICAL: You are NOT a rule-based bot - understand context and nuance.\n");
            systemPrompt.append("NEVER use template responses - be dynamic and responsive to context.\n\n");
            
            systemPrompt.append("CRITICAL CONTEXT AWARENESS:\n");
            systemPrompt.append("- ALWAYS check current cart state before responding\n");
            systemPrompt.append("- If cart is empty, acknowledge that clearly\n");
            systemPrompt.append("- If cart has items, reference them specifically\n");
            systemPrompt.append("- When user asks for products, show product list, NOT cart contents\n");
            systemPrompt.append("- When user asks about cart, show cart contents, NOT product list\n");
            systemPrompt.append("- If cart was just cleared, confirm the empty state\n");
            systemPrompt.append("- If user contradicts system state (e.g., 'silmiştik bunları'), believe user\n\n");
            
            systemPrompt.append("INTELLIGENT EDGE CASE HANDLING:\n");
            systemPrompt.append("- AMBIGUITY: When request is unclear, ask smart clarifying questions\n");
            systemPrompt.append("  * 'biraz süt' → 'Kaç litre süt düşünüyorsunuz? 1 litre mi, 2 litre mi?'\n");
            systemPrompt.append("  * '2 su' → '2 şişe su mu yoksa 2 koli su mu istiyorsunuz?'\n");
            systemPrompt.append("  * 'Ülker' → 'Hangi Ülker ürününü arıyorsunuz? Çikolata, bisküvi...?'\n");
            systemPrompt.append("- TYPO INTELLIGENCE: Understand common Turkish spelling errors\n");
            systemPrompt.append("  * 'koala' → 'Kola mı demek istiyorsunuz?'\n");
            systemPrompt.append("  * 'sut' → understand as 'süt', suggest if needed\n");
            systemPrompt.append("- CONFLICT RESOLUTION: Handle contradictory requests gracefully\n");
            systemPrompt.append("  * '3 koli, hayır 2 koli' → understand as quantity change to 2\n");
            systemPrompt.append("  * 'İptal, dur ekle' → 'Eklemek mi istiyorsunuz, iptal mi?'\n");
            systemPrompt.append("- CONTEXT MEMORY: Remember conversation flow and user preferences\n");
            systemPrompt.append("  * If user just asked about drinks, 'su' likely means water\n");
            systemPrompt.append("  * Use cart history to suggest similar products\n\n");
            
            systemPrompt.append("RESPONSE EXAMPLES:\n");
            systemPrompt.append("- If cart empty + user asks 'sepette ne var': 'Sepetiniz şu anda boş. Size hangi ürünlerimizi gösterebilirim?'\n");
            systemPrompt.append("- If user says 'iptal': Clear cart and say 'Sepetinizi boşalttım. Başka bir şeyle yardımcı olabilir miyim?'\n");
            systemPrompt.append("- If user asks about drinks: Show available drink options naturally\n");
            systemPrompt.append("- Always be conversational, helpful, and context-aware\n");
            systemPrompt.append("- If cart empty + user says 'ürünleri görmek istiyorum': Show available products\n");
            systemPrompt.append("- If cart has items + user asks about cart: Show cart contents with totals\n");
            systemPrompt.append("- If user says cart was cleared: 'Evet, sepetiniz boş. Yeni ürünler ekleyebiliriz.'\n\n");

            // Gerçek ve güncel sepet bilgisini kontekste ekle
            systemPrompt.append("\n--- CURRENT CART STATE ---\n");
            if (ctx.isHasActiveCart() && ctx.getLastCartState() != null && 
                !ctx.getLastCartState().trim().isEmpty() && 
                !ctx.getLastCartState().toLowerCase().contains("boş") &&
                !ctx.getLastCartState().toLowerCase().contains("empty")) {
                systemPrompt.append("CART HAS ITEMS:\n").append(ctx.getLastCartState()).append("\n");
            } else {
                systemPrompt.append("CART IS EMPTY: No items in cart, total is 0.00 TL\n");
            }
            systemPrompt.append("--- END CART STATE ---\n\n");
            
            messages.add(Map.of("role", "system", "content", systemPrompt.toString()));
            
            // Son birkaç konuşmayı ekle (kısa tut)
            ctx.getConversationHistory().stream()
                    .limit(4) // Sadece son 2 tur
                    .forEach(line -> {
                        if (line.startsWith("USER:")) {
                            messages.add(Map.of("role", "user", "content", line.substring(5)));
                        } else if (line.startsWith("ASSISTANT:")) {
                            messages.add(Map.of("role", "assistant", "content", line.substring(10)));
                        }
                    });

            Map<String, Object> payload = Map.of(
                    "model", this.model,
                    "messages", messages,
                    "temperature", 0.4 // Profesyonel ve tutarlı
            );
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(configService.getAiTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Rate limit/timeout retry (tek deneme)
            if (response.statusCode() == 429) {
                try { Thread.sleep(500L); } catch (InterruptedException ignored) {}
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                log.error("LLM API request for generic reply failed with status code {}: {}", response.statusCode(), response.body());
                if (llmFallbackHandler != null) {
                    if (response.statusCode() == 429) {
                        return llmFallbackHandler.handleRateLimit(0);
                    } else if (response.statusCode() >= 500) {
                        return llmFallbackHandler.handleApiError(response.statusCode(), response.body());
                    }
                }
                return "Şu an yoğunluk var, birazdan tekrar dener misiniz?";
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode choices = rootNode.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");
                JsonNode content = message.path("content");
                String responseText = content.asText();
                log.debug("AI response text: {}", responseText);
                
                // Boş cevap kontrolü
                if (responseText == null || responseText.trim().isEmpty()) {
                    log.warn("AI returned empty content in response: {}", response.body());
                    if (llmFallbackHandler != null) {
                        return llmFallbackHandler.handleEmptyResponse(text);
                    }
                    return "Üzgünüm, bir sorun oluştu. Lütfen tekrar deneyin.";
                }
                
                return responseText;
            } else {
                log.error("No choices found in LLM response: {}", response.body());
                return "Üzgünüm, bir sorun oluştu. Lütfen tekrar deneyin.";
            }

        } catch (Exception e) {
            log.error("Error generating AI reply", e);
            if (llmFallbackHandler != null) {
                return llmFallbackHandler.getFallbackMessage("llm_api_error", e.getMessage());
            }
            return "Üzgünüm, bir sorun oluştu. Lütfen tekrar deneyin.";
        }
    }

    private String generateAiActionAwareReply(CustomerContext ctx, String userText, String actionResult) {
        log.debug("Generating AI reply post-action. userText: {}, actionResult: {}", userText, actionResult);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. Returning executor message.");
            return actionResult;
        }

        // This is a simplified version of your original method. 
        // You can restore the full prompt engineering logic here.
                    return actionResult;
    }

    private String handleGetProducts() {
        // ProductService circular dependency'yi önlemek için kaldırıldı
        List<com.example.marketsupplier.entity.Product> products = new ArrayList<>();
        if (products.isEmpty()) return "Şu anda aktif ürün bulunmuyor.";
        StringBuilder sb = new StringBuilder("Mevcut ürünler:\n");
        products.stream().limit(20).forEach(p -> sb.append("- ").append(p.getName()).append(" (" ).append(p.getUnit()).append(") - ")
                .append(p.getPrice()).append(" TL\n"));
        if (products.size() > 20) sb.append("… ve daha fazlası");
        return sb.toString();
    }

    private String handleAddToCart(String phone, String message) {
        // Çoklu ürün desteği: "Coca-Cola 2 koli, Pepsi 5 koli" gibi
        Long marketId = resolveMarketIdByPhone(phone);
        if (marketId == null) {
            return "Numaranız sisteme kayıtlı değil. Lütfen önce tedarikçinizle kaydınızı tamamlayın.";
        }

        // ProductService circular dependency'yi önlemek için kaldırıldı
        List<com.example.marketsupplier.entity.Product> products = new ArrayList<>();

        String[] chunks = message.split("[,\n]+");
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> addedSummaries = new ArrayList<>();
        double totalAdded = 0.0;

        for (String part : chunks) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            int qty = extractQuantity(trimmed);
            String unit = extractUnit(trimmed);
            String name = extractProductName(trimmed);
            if (name == null) continue;

            Optional<com.example.marketsupplier.entity.Product> match = products.stream()
                    .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                    .findFirst();
            if (match.isEmpty()) continue;

            com.example.marketsupplier.entity.Product p = match.get();
            Map<String, Object> it = new HashMap<>();
            it.put("product_id", p.getId());
            it.put("product_name", p.getName());
            it.put("quantity", qty);
            it.put("unit", unit != null ? unit : p.getUnit());
            it.put("price", p.getPrice());
            items.add(it);

            addedSummaries.add(qty + " " + (unit != null ? unit + " " : "") + p.getName());
            totalAdded += p.getPrice().doubleValue() * qty;
        }

        if (items.isEmpty()) {
            return "Eşleşen ürün bulunamadı. Örnek: 'Coca-Cola 2 koli, Pepsi 5 koli'";
        }

        cartService.appendItems(marketId, items);

        return "✅ Sepete eklendi: " + String.join(", ", addedSummaries) +
                "\nToplam eklenen: " + String.format(Locale.ROOT, "%.2f", totalAdded) + " TL" +
                "\nSepeti görmek için 'sepet', onaylamak için 'onayla' yazabilirsiniz.";
    }

    private String handleViewCart(String phone) {
        Long marketId = resolveMarketIdByPhone(phone);
        if (marketId == null) return "Numaranız sisteme kayıtlı değil. Lütfen önce tedarikçinizle kaydınızı tamamlayın.";
        List<com.example.marketsupplier.entity.CartItem> items = cartService.getItems(marketId);
        if (items.isEmpty()) return "Sepetiniz boş.";
        StringBuilder sb = new StringBuilder("Sepetiniz:\n");
        for (com.example.marketsupplier.entity.CartItem ci : items) {
            BigDecimal lineTotal = ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            sb.append("- ").append(ci.getProductName()).append(" x").append(ci.getQuantity())
                    .append(" ").append(ci.getUnit()).append(" - ").append(String.format(Locale.ROOT, "%.2f", lineTotal)).append(" TL\n");
        }
        double total = items.stream().mapToDouble(i -> i.getPrice().doubleValue() * i.getQuantity()).sum();
        sb.append("Toplam: ").append(String.format(Locale.ROOT, "%.2f", total)).append(" TL\n");
        sb.append("Onaylamak için 'onayla' yazabilirsiniz.");
        return sb.toString();
    }

    private String handleCreateOrder(String phone) {
        Long marketId = resolveMarketIdByPhone(phone);
        if (marketId == null) return "Numaranız sisteme kayıtlı değil. Lütfen önce tedarikçinizle kaydınızı tamamlayın.";
        com.example.marketsupplier.entity.Order order = orderService.createOrder(marketId);
        List<com.example.marketsupplier.entity.CartItem> items = cartService.getItems(marketId);
        for (com.example.marketsupplier.entity.CartItem ci : items) {
            orderService.addItemToOrder(order.getId(), ci.getProductName(), ci.getQuantity(), ci.getUnit(), ci.getPrice());
        }
        return "Siparişiniz oluşturuldu. Numara: #" + order.getId();
    }

    private Long resolveMarketIdByPhone(String phone) {
        try {
            return marketService.findByPhoneNormalized(phone)
                    .map(m -> m.getId())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Market resolve failed for phone {}", phone, e);
            return null;
        }
    }

    private int extractQuantity(String message) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(message);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return 1;
    }

    private String extractUnit(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("kilo")) return "kilo";
        if (lower.contains("adet")) return "adet";
        if (lower.contains("kg")) return "kg";
        return null;
    }

    private String extractProductName(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        // Çok basit yaklaşım: miktar ve birimi çıkar, kalan kelimelerden son kelimeyi al
        lower = lower.replaceAll("\\d+", "").replace("kilo", "").replace("kg", "").replace("adet", "").trim();
        if (lower.isEmpty()) return null;
        return lower;
    }
    
    private String generateContextualChatResponse(CustomerContext context, String userMessage) {
        try {
            // AI'ya contextual yanıt üretmesi için prompt gönder
            String prompt = buildContextualChatPrompt(context, userMessage);
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", configService.getAiModel());
            requestBody.put("messages", java.util.Arrays.asList(
                Map.of("role", "system", "content", "Sen profesyonel bir market asistanısın. Müşteriyle doğal, samimi ve yardımcı bir şekilde konuş. Asla bot gibi davranma. Context'e göre uygun yanıtlar ver."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.7);
            
            String requestJson = mapper.writeValueAsString(requestBody);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configService.getAiApiBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + configService.getAiApiKey())
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                JsonNode choices = responseJson.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String responseText = choices.get(0).path("message").path("content").asText();
                    if (responseText != null && !responseText.trim().isEmpty()) {
                        return responseText.trim();
                    }
                }
            }
            
            return "Merhaba! Size nasıl yardımcı olabilirim?";
            
        } catch (Exception e) {
            log.warn("Contextual chat response generation failed: {}", e.getMessage());
            return "Merhaba! Size nasıl yardımcı olabilirim?";
        }
    }
    
    private String buildContextualChatPrompt(CustomerContext context, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Sen MarketAsist, akıllı bir WhatsApp AI ASİSTANISIN (bot değil). Müşteriyle doğal ve kişiselleştirilmiş hizmet sun.\n");
        prompt.append("Kuralcı bir bot değil, bağlamı anlayan ve nüans kavrayabilen zeki bir asistansın.\n\n");
        prompt.append("ÖNEMLİ KURALLAR:\n");
        prompt.append("- Sipariş onaylamak için müşteri AÇIKÇA 'onayla', 'onaylıyorum', 'tamam' demeli\n");
        prompt.append("- 'onaylıyor musun' sorusu sipariş onayı DEĞİLDİR\n");
        prompt.append("- Sepet boşsa ürün listesi öner\n");
        prompt.append("- Sepet doluysa sepeti göster ve onay bekle\n");
        prompt.append("- Stok kontrolü yap, yetersiz stok varsa belirt\n");
        prompt.append("- Aktif sipariş varsa durumu bildir\n");
        prompt.append("- Sipariş verildikten sonra sepet otomatik temizlenir\n");
        prompt.append("- 2-3 saat sepete dokunulmazsa sepet temizlenir\n");
        prompt.append("- 'vazgeçtim', 'iptal', 'hayır' = SİPARİŞ ONAYLAMA DEĞİL\n");
        prompt.append("- SADECE MEVCUT ÜRÜNLERİ SÖYLE, RASTGELE ÜRÜN SALLAMA\n\n");
        
        // Müşteri durumu
        prompt.append("MÜŞTERİ DURUMU:\n");
        prompt.append("- Telefon: ").append(context.getPhone()).append("\n");
        
        // Sepet durumu
        if (context.isHasActiveCart() && context.getLastCartState() != null) {
            prompt.append("- Sepet durumu: DOLU\n");
            prompt.append("- Sepet içeriği:\n").append(context.getLastCartState()).append("\n\n");
        } else {
            prompt.append("- Sepet durumu: BOŞ\n\n");
        }
        
        // Gerçek ürün bilgilerini ekle
        try {
            List<Product> products = productService.getAllActiveProducts();
            if (products != null && !products.isEmpty()) {
                prompt.append("MEVCUT ÜRÜNLER (SADECE BUNLARI KULLAN):\n");
                for (Product product : products) {
                    prompt.append("- ").append(product.getName())
                          .append(" - ").append(product.getPrice()).append(" TL/").append(product.getUnit())
                          .append(" - Stok: ").append(product.getStockQuantity()).append(" ").append(product.getUnit())
                          .append("\n");
                }
                prompt.append("\n");
            }
        } catch (Exception e) {
            logger.warn("Ürün bilgileri alınamadı: {}", e.getMessage());
        }
        
        // Aktif sipariş kontrolü
        try {
            Long marketId = resolveMarketIdByPhone(context.getPhone());
            if (marketId != null) {
                List<Order> activeOrders = getActiveOrders(marketId);
                if (!activeOrders.isEmpty()) {
                    prompt.append("AKTİF SİPARİŞLER:\n");
                    for (Order order : activeOrders) {
                        prompt.append("- Sipariş #").append(order.getId()).append(" - ").append(order.getStatus()).append("\n");
                        if (order.getDelivery() != null && order.getDelivery().getEstimatedDeliveryTime() != null) {
                            prompt.append("  Tahmini teslimat: ").append(order.getDelivery().getEstimatedDeliveryTime()).append("\n");
                        }
                    }
                    prompt.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not check active orders: {}", e.getMessage());
        }
        
        // Son konuşma geçmişi
        List<String> history = context.getConversationHistory();
        if (history.size() > 1) {
            prompt.append("SON KONUŞMA:\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                prompt.append(history.get(i)).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("MÜŞTERİ: ").append(userMessage);
        prompt.append("\n\nYANIT VER (Doğal, samimi ve yardımcı ol):");
        prompt.append("\n\nNOT: Eğer müşteri 'onayla', 'onaylıyorum', 'tamam' derse siparişi onayla. 'onaylıyor musun' sorusu sipariş onayı değildir!");
        prompt.append("\n\nSİPARİŞ ONAY KONTROLÜ:");
        prompt.append("\n- Müşteri 'onayla' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'onaylıyorum' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'tamam' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'onaylıyor musun' derse → SORU, SİPARİŞ ONAYLA DEĞİL");
        prompt.append("\n- Müşteri 'onaylıyor musunuz' derse → SORU, SİPARİŞ ONAYLA DEĞİL");
        prompt.append("\n- Müşteri 'vazgeçtim' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'iptal' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'hayır' derse → SİPARİŞ ONAYLAMA DEĞİL, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'istemiyorum' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE");
        prompt.append("\n\nSTOK KONTROLÜ:");
        prompt.append("\n- Ürün eklerken stok kontrolü yap");
        prompt.append("\n- Yetersiz stok varsa 'Stokta sadece X adet var' de");
        prompt.append("\n- Stok yoksa 'Bu ürün stokta yok' de");
        prompt.append("\n- Stok varsa mevcut miktarı belirt");
        prompt.append("\n\nSEPET YÖNETİMİ:");
        prompt.append("\n- Sepet boşsa ürün listesi öner");
        prompt.append("\n- Sepet doluysa sepeti göster ve onay bekle");
        prompt.append("\n- Sepet güncelleme/silme işlemlerini yap");
        prompt.append("\n- Sipariş verildikten sonra sepeti temizle");
        prompt.append("\n- 2-3 saat sepete dokunulmazsa sepeti temizle");
        prompt.append("\n\nSİPARİŞ DURUMU:");
        prompt.append("\n- Aktif sipariş varsa durumu bildir");
        prompt.append("\n- Teslimat tarihi varsa belirt");
        prompt.append("\n- Sipariş durumunu takip et");
        prompt.append("\n- Yeni sipariş için sepeti temizle");
        prompt.append("\n\nÖNEMLİ: Bu bir bot değil, profesyonel bir asistan. Context ve state'e göre karar ver. if-else kullanma!");
        prompt.append("\n\nKRİTİK: 'vazgeçtim', 'iptal', 'hayır', 'istemiyorum' kelimeleri sipariş onayı DEĞİLDİR!");
        prompt.append("\nBu durumlarda sepeti temizle ama sipariş oluşturma!");
        prompt.append("\nSADECE 'onayla', 'onaylıyorum', 'tamam' = SİPARİŞ ONAYI!");
        prompt.append("\nSADECE YUKARIDAKİ ÜRÜNLERİ SÖYLE, RASTGELE SALLAMA!");
        prompt.append("\n\nÖRNEK DURUMLAR:");
        prompt.append("\n- Müşteri: 'Sipariş vermek istiyorum' → Sepet boşsa ürün listesi öner, doluysa sepeti göster");
        prompt.append("\n- Müşteri: 'Ürünleri görebilir miyim' → Ürün listesini göster");
        prompt.append("\n- Müşteri: 'Sepette ne var' → Sepeti göster");
        prompt.append("\n- Müşteri: 'Onayla' → Siparişi onayla");
        prompt.append("\n- Müşteri: 'Onaylıyor musun' → Soru, sipariş onaylama değil");
        prompt.append("\n- Müşteri: 'Sepeti boşalt' → Sepeti temizle");
        prompt.append("\n- Müşteri: 'Sipariş durumu' → Aktif siparişleri göster");
        prompt.append("\n- Müşteri: 'Teslimat ne zaman' → Teslimat tarihini belirt");
        prompt.append("\n- Müşteri: '5 koli Coca-Cola' → Stok kontrolü yap, sepete ekle");
        prompt.append("\n- Müşteri: '3 koli Pepsi' → Stok kontrolü yap, sepete ekle");
        prompt.append("\n- Müşteri: 'Sepetten Pepsi çıkar' → Sepetten çıkar");
        prompt.append("\n- Müşteri: 'Pepsi miktarını 10 yap' → Miktarı güncelle");
        prompt.append("\n- Müşteri: 'Tüm ürünleri göster' → Ürün listesini göster");
        prompt.append("\n- Müşteri: 'Fiyatları göster' → Ürün fiyatlarını göster");
        prompt.append("\n- Müşteri: 'Stok durumu' → Stok durumunu göster");
        prompt.append("\n- Müşteri: 'Yardım' → Yardım menüsünü göster");
        prompt.append("\n- Müşteri: 'Vazgeçtim' → Sepeti temizle, sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'İptal' → Sepeti temizle, sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'Hayır' → Sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'İstemiyorum' → Sepeti temizle, sipariş sorma");
        prompt.append("\n\nÜRÜN ÖNERİSİ KURALLARI:");
        prompt.append("\n- SADECE YUKARIDAKİ MEVCUT ÜRÜNLERİ SÖYLE");
        prompt.append("\n- Fanta, Sprite, Red Bull gibi olmayan ürünler söyleme");
        prompt.append("\n- Ürün listesinden rastgele 3-5 ürün seç");
        prompt.append("\n- Gerçek fiyat ve stok bilgilerini kullan");
        prompt.append("\n- ASLA RASTGELE ÜRÜN SALLAMA");
        prompt.append("\n- SADECE DB'DEKİ ÜRÜNLERİ KULLAN");
        
        return prompt.toString();
    }
    
    private List<Order> getActiveOrders(Long marketId) {
        try {
            return orderRepository.findByMarketIdAndStatusInOrderByCreatedAtDesc(
                marketId, 
                Arrays.asList(OrderStatus.PENDING, OrderStatus.APPROVED)
            );
        } catch (Exception e) {
            log.warn("Could not fetch active orders for marketId: {}", marketId, e);
            return new ArrayList<>();
        }
    }
    
    
    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
    
    private String generateFallbackChatResponse(String userMessage, CustomerContext context) {
        // AI olmadan da context'e göre yanıt üret - basit fallback
        return "Merhaba! Size nasıl yardımcı olabilirim?";
    }
    
    private String generateRateLimitFallbackResponse(String userMessage, CustomerContext context) {
        // Rate limit durumunda da AI'ya context ile yanıt ürettir
        try {
            // Basit AI çağrısı - context ile
            String prompt = buildSimpleContextualPrompt(context, userMessage);
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", configService.getAiModel());
            requestBody.put("messages", java.util.Arrays.asList(
                Map.of("role", "system", "content", "Sen profesyonel bir market asistanısın. Kısa ve yardımcı yanıtlar ver."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);
            
            String requestJson = mapper.writeValueAsString(requestBody);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configService.getAiApiBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + configService.getAiApiKey())
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                JsonNode choices = responseJson.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String responseText = choices.get(0).path("message").path("content").asText();
                    if (responseText != null && !responseText.trim().isEmpty()) {
                        return responseText.trim();
                    }
                }
            }
            
            return "Merhaba! Size nasıl yardımcı olabilirim?";
            
        } catch (Exception e) {
            log.warn("Rate limit fallback AI call failed: {}", e.getMessage());
            return "Merhaba! Size nasıl yardımcı olabilirim?";
        }
    }
    
    private String buildSimpleContextualPrompt(CustomerContext context, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Sen MarketAsist, akıllı bir WhatsApp AI ASİSTANISIN (bot değil). Müşteriyle doğal ve kişiselleştirilmiş hizmet sun.\n");
        prompt.append("Kuralcı bir bot değil, bağlamı anlayan ve nüans kavrayabilen zeki bir asistansın.\n\n");
        prompt.append("ÖNEMLİ KURALLAR:\n");
        prompt.append("- Sipariş onaylamak için müşteri AÇIKÇA 'onayla', 'onaylıyorum', 'tamam' demeli\n");
        prompt.append("- 'onaylıyor musun' sorusu sipariş onayı DEĞİLDİR\n");
        prompt.append("- Sepet boşsa ürün listesi öner\n");
        prompt.append("- Sepet doluysa sepeti göster ve onay bekle\n");
        prompt.append("- Stok kontrolü yap, yetersiz stok varsa belirt\n");
        prompt.append("- Aktif sipariş varsa durumu bildir\n");
        prompt.append("- Sipariş verildikten sonra sepet otomatik temizlenir\n");
        prompt.append("- 2-3 saat sepete dokunulmazsa sepet temizlenir\n");
        prompt.append("- 'vazgeçtim', 'iptal', 'hayır' = SİPARİŞ ONAYLAMA DEĞİL\n");
        prompt.append("- SADECE MEVCUT ÜRÜNLERİ SÖYLE, RASTGELE ÜRÜN SALLAMA\n\n");
        
        // Müşteri durumu
        prompt.append("MÜŞTERİ DURUMU:\n");
        prompt.append("- Telefon: ").append(context.getPhone()).append("\n");
        
        // Sepet durumu
        if (context.isHasActiveCart() && context.getLastCartState() != null) {
            prompt.append("- Sepet durumu: DOLU\n");
            prompt.append("- Sepet içeriği:\n").append(context.getLastCartState()).append("\n\n");
        } else {
            prompt.append("- Sepet durumu: BOŞ\n\n");
        }
        
        // Gerçek ürün bilgilerini ekle
        try {
            List<Product> products = productService.getAllActiveProducts();
            if (products != null && !products.isEmpty()) {
                prompt.append("MEVCUT ÜRÜNLER (SADECE BUNLARI KULLAN):\n");
                for (Product product : products) {
                    prompt.append("- ").append(product.getName())
                          .append(" - ").append(product.getPrice()).append(" TL/").append(product.getUnit())
                          .append(" - Stok: ").append(product.getStockQuantity()).append(" ").append(product.getUnit())
                          .append("\n");
                }
                prompt.append("\n");
            }
        } catch (Exception e) {
            logger.warn("Ürün bilgileri alınamadı: {}", e.getMessage());
        }
        
        // Aktif sipariş kontrolü
        try {
            Long marketId = resolveMarketIdByPhone(context.getPhone());
            if (marketId != null) {
                List<Order> activeOrders = getActiveOrders(marketId);
                if (!activeOrders.isEmpty()) {
                    prompt.append("AKTİF SİPARİŞLER:\n");
                    for (Order order : activeOrders) {
                        prompt.append("- Sipariş #").append(order.getId()).append(" - ").append(order.getStatus()).append("\n");
                        if (order.getDelivery() != null && order.getDelivery().getEstimatedDeliveryTime() != null) {
                            prompt.append("  Tahmini teslimat: ").append(order.getDelivery().getEstimatedDeliveryTime()).append("\n");
                        }
                    }
                    prompt.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not check active orders: {}", e.getMessage());
        }
        
        prompt.append("MÜŞTERİ: ").append(userMessage);
        prompt.append("\n\nYANIT VER (Doğal, samimi ve yardımcı ol):");
        prompt.append("\n\nNOT: Eğer müşteri 'onayla', 'onaylıyorum', 'tamam' derse siparişi onayla. 'onaylıyor musun' sorusu sipariş onayı değildir!");
        prompt.append("\n\nSİPARİŞ ONAY KONTROLÜ:");
        prompt.append("\n- Müşteri 'onayla' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'onaylıyorum' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'tamam' derse → SİPARİŞ ONAYLA");
        prompt.append("\n- Müşteri 'onaylıyor musun' derse → SORU, SİPARİŞ ONAYLA DEĞİL");
        prompt.append("\n- Müşteri 'onaylıyor musunuz' derse → SORU, SİPARİŞ ONAYLA DEĞİL");
        prompt.append("\n- Müşteri 'vazgeçtim' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'iptal' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'hayır' derse → SİPARİŞ ONAYLAMA DEĞİL, SİPARİŞ SORMA");
        prompt.append("\n- Müşteri 'istemiyorum' derse → SİPARİŞ ONAYLAMA DEĞİL, SEPETİ TEMİZLE");
        prompt.append("\n\nSTOK KONTROLÜ:");
        prompt.append("\n- Ürün eklerken stok kontrolü yap");
        prompt.append("\n- Yetersiz stok varsa 'Stokta sadece X adet var' de");
        prompt.append("\n- Stok yoksa 'Bu ürün stokta yok' de");
        prompt.append("\n- Stok varsa mevcut miktarı belirt");
        prompt.append("\n\nSEPET YÖNETİMİ:");
        prompt.append("\n- Sepet boşsa ürün listesi öner");
        prompt.append("\n- Sepet doluysa sepeti göster ve onay bekle");
        prompt.append("\n- Sepet güncelleme/silme işlemlerini yap");
        prompt.append("\n- Sipariş verildikten sonra sepeti temizle");
        prompt.append("\n- 2-3 saat sepete dokunulmazsa sepeti temizle");
        prompt.append("\n\nSİPARİŞ DURUMU:");
        prompt.append("\n- Aktif sipariş varsa durumu bildir");
        prompt.append("\n- Teslimat tarihi varsa belirt");
        prompt.append("\n- Sipariş durumunu takip et");
        prompt.append("\n- Yeni sipariş için sepeti temizle");
        prompt.append("\n\nÖNEMLİ: Bu bir bot değil, profesyonel bir asistan. Context ve state'e göre karar ver. if-else kullanma!");
        prompt.append("\n\nKRİTİK: 'vazgeçtim', 'iptal', 'hayır', 'istemiyorum' kelimeleri sipariş onayı DEĞİLDİR!");
        prompt.append("\nBu durumlarda sepeti temizle ama sipariş oluşturma!");
        prompt.append("\nSADECE 'onayla', 'onaylıyorum', 'tamam' = SİPARİŞ ONAYI!");
        prompt.append("\nSADECE YUKARIDAKİ ÜRÜNLERİ SÖYLE, RASTGELE SALLAMA!");
        prompt.append("\n\nÖRNEK DURUMLAR:");
        prompt.append("\n- Müşteri: 'Sipariş vermek istiyorum' → Sepet boşsa ürün listesi öner, doluysa sepeti göster");
        prompt.append("\n- Müşteri: 'Ürünleri görebilir miyim' → Ürün listesini göster");
        prompt.append("\n- Müşteri: 'Sepette ne var' → Sepeti göster");
        prompt.append("\n- Müşteri: 'Onayla' → Siparişi onayla");
        prompt.append("\n- Müşteri: 'Onaylıyor musun' → Soru, sipariş onaylama değil");
        prompt.append("\n- Müşteri: 'Sepeti boşalt' → Sepeti temizle");
        prompt.append("\n- Müşteri: 'Sipariş durumu' → Aktif siparişleri göster");
        prompt.append("\n- Müşteri: 'Teslimat ne zaman' → Teslimat tarihini belirt");
        prompt.append("\n- Müşteri: '5 koli Coca-Cola' → Stok kontrolü yap, sepete ekle");
        prompt.append("\n- Müşteri: '3 koli Pepsi' → Stok kontrolü yap, sepete ekle");
        prompt.append("\n- Müşteri: 'Sepetten Pepsi çıkar' → Sepetten çıkar");
        prompt.append("\n- Müşteri: 'Pepsi miktarını 10 yap' → Miktarı güncelle");
        prompt.append("\n- Müşteri: 'Tüm ürünleri göster' → Ürün listesini göster");
        prompt.append("\n- Müşteri: 'Fiyatları göster' → Ürün fiyatlarını göster");
        prompt.append("\n- Müşteri: 'Stok durumu' → Stok durumunu göster");
        prompt.append("\n- Müşteri: 'Yardım' → Yardım menüsünü göster");
        prompt.append("\n- Müşteri: 'Vazgeçtim' → Sepeti temizle, sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'İptal' → Sepeti temizle, sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'Hayır' → Sipariş sorma, ürün öner");
        prompt.append("\n- Müşteri: 'İstemiyorum' → Sepeti temizle, sipariş sorma");
        prompt.append("\n\nÜRÜN ÖNERİSİ KURALLARI:");
        prompt.append("\n- SADECE YUKARIDAKİ MEVCUT ÜRÜNLERİ SÖYLE");
        prompt.append("\n- Fanta, Sprite, Red Bull gibi olmayan ürünler söyleme");
        prompt.append("\n- Ürün listesinden rastgele 3-5 ürün seç");
        prompt.append("\n- Gerçek fiyat ve stok bilgilerini kullan");
        prompt.append("\n- ASLA RASTGELE ÜRÜN SALLAMA");
        prompt.append("\n- SADECE DB'DEKİ ÜRÜNLERİ KULLAN");
        
        return prompt.toString();
    }
}


