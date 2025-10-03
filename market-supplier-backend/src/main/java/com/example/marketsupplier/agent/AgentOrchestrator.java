package com.example.marketsupplier.agent;

import com.example.marketsupplier.agent.config.AgentConfig;
import com.example.marketsupplier.agent.dialog.DialogManager;
import com.example.marketsupplier.agent.dialog.DialogManager.PlannedAction;
import com.example.marketsupplier.agent.executor.ActionExecutor;
import com.example.marketsupplier.agent.nlp.IntentExtractor;
import com.example.marketsupplier.agent.nlp.LLMIntentExtractor;
import com.example.marketsupplier.agent.nlp.IntentClassifier;
import com.example.marketsupplier.agent.nlp.IntentConfigLoader;
import com.example.marketsupplier.agent.actions.ActionHandler;
import com.example.marketsupplier.service.CustomerContext;
import com.example.marketsupplier.agent.context.ContextManager.CartContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentOrchestrator {
    
    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final IntentExtractor nlu;
    private final DialogManager dialog;
    private final ActionExecutor executor;
    private final AgentConfig config;
    private final IntentClassifier intentClassifier;
    private final IntentConfigLoader intentConfigLoader;
    private final java.util.List<ActionHandler> actionHandlers;

    // Onay akışını çoklu instance için Redis ile tut
    @Autowired(required = false)
    private RedisTemplate<String, Object> redis;

    // Redis yoksa tek-instance fallback için hafif TTL cache (sadece geçici)
    private final java.util.concurrent.ConcurrentHashMap<String, Long> localAwaitingUntilMs = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<String, Boolean> awaitingConfirmation = new ConcurrentHashMap<>();

    public AgentOrchestrator(LLMIntentExtractor llmNlu,
                          DialogManager dialog, ActionExecutor executor, 
                          AgentConfig config, IntentClassifier intentClassifier,
                          IntentConfigLoader intentConfigLoader,
                          java.util.List<ActionHandler> actionHandlers) {
        this.nlu = llmNlu;
        this.dialog = dialog;
        this.executor = executor;
        this.config = config;
        this.intentClassifier = intentClassifier;
        this.intentConfigLoader = intentConfigLoader;
        this.actionHandlers = actionHandlers == null ? java.util.Collections.emptyList() : actionHandlers;
    }

    // WhatsApp sayfalama gibi durumlarda servis katmanından kullanmak için
    public ActionExecutor getExecutor() {
        return this.executor;
    }

    public OrchestrationResult handle(String phone, String text, CustomerContext context) {
        IntentClassifier.IntentResult intentResult = intentClassifier.classify(text, context);
        String intent = intentResult.getIntent();
        String response;
        
        // AI tabanlı niyet analizi - tamamen context ve state'e dayalı
        log.info("AI Intent Classification: text='{}' -> intent='{}', confidence={}", text, intentResult.getIntent(), intentResult.getConfidence());
        
        // Context'i güncelle (onay bekleme durumu)
        if (context != null) {
            context.getSessionData().put("awaiting_confirmation", isAwaiting(phone));
        }
        
        // MODE STATE - AI'nın belirlediği intent'e göre mod kontrolü
        String mode = "ordering"; // default ordering_with_ai
        if (context != null && context.getSessionData() != null && context.getSessionData().get("mode") != null) {
            mode = String.valueOf(context.getSessionData().get("mode"));
        }
        
        // Mod seçimi - AI'nın belirlediği intent
        if ("show_modes".equals(intentResult.getIntent())) {
            return new OrchestrationResult("show_modes", "SHOW_MODES_REQUEST", false); // AI'nın işleyeceği
        }
        
        // Mod değişimi - sadece AI'nın algıladığı açık mod değişim istekleri
        if (intentResult.getIntent().startsWith("change_mode_")) {
            String newMode = intentResult.getIntent().substring("change_mode_".length());
            if (context != null) {
                context.getSessionData().put("mode", newMode);
                return new OrchestrationResult("change_mode_" + newMode, "MODE_CHANGED_" + newMode, false); // AI'nın contextual yanıt üreteceği
            }
        }
        
        // Chat modunda - AI'nın belirlediği intent'lere göre karar ver
        if ("chat".equals(mode) && !"chat".equals(intentResult.getIntent())) {
            // AI'nın chat modunda olmayan bir intent tespit etmesi durumunda kontekstual uyarı
            return new OrchestrationResult("INTENT_CHAT_MODE_RESTRICTED", "INTENT_CHAT_MODE_RESTRICTED", false);
        }
        
        // Özel durumları işle
        switch (intentResult.getIntent()) {
            case "confirm":
                if (isAwaiting(phone)) {
                    setAwaiting(phone, false);
                    return new OrchestrationResult("confirm", executor.confirmOrder(phone), false);
                }
                return new OrchestrationResult("confirm", "Onay beklenen bir işlem bulunmuyor.", false);
                
            case "cancel":
                if (isAwaiting(phone)) {
                    setAwaiting(phone, false);
                    return new OrchestrationResult("cancel", "Onayı iptal ettim. Siparişinizi oluşturmadım. Sepeti güncellemek ister misiniz?", false);
                }
                // Eğer onay beklenmiyorsa, sepeti temizle
                return new OrchestrationResult("clear_cart", executor.clearCart(phone), false);
                
            case "clarify":
                return new OrchestrationResult("clarify", intentResult.getDescription(), false);
                
            case "navigate":
                if (context != null) {
                    return new OrchestrationResult("navigate", executor.showNextProducts(context), false);
                }
                return new OrchestrationResult("navigate", executor.showProducts(), false);
                
            case "show_product_category":
                String categoryQuery = intentResult.getDescription();
                if (categoryQuery == null || categoryQuery.trim().isEmpty()) {
                    categoryQuery = text; // Fallback to original text
                }
                return new OrchestrationResult("show_product_category", executor.showProductsByCategory(categoryQuery), false);
                
            case "show_alternatives":
                String comparisonQuery = intentResult.getDescription();
                if (comparisonQuery == null || comparisonQuery.trim().isEmpty()) {
                    comparisonQuery = text; // Fallback to original text
                }
                // Get conversation context for reference
                String referenceContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("show_alternatives", executor.showAlternatives(phone, comparisonQuery, referenceContext), false);
                
            case "clarify_quantity":
                String quantityQuery = intentResult.getDescription();
                if (quantityQuery == null || quantityQuery.trim().isEmpty()) {
                    quantityQuery = text; // Fallback to original text
                }
                // Get product context from conversation
                String productContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("clarify_quantity", executor.clarifyQuantity(phone, quantityQuery, productContext), false);
                
            case "emotional_support":
                String emotionalQuery = intentResult.getDescription();
                if (emotionalQuery == null || emotionalQuery.trim().isEmpty()) {
                    emotionalQuery = text;
                }
                String emotionalContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("emotional_support", executor.handleEmotionalSupport(phone, emotionalQuery, emotionalContext), false);
                
            case "confident_purchase":
                String confidenceQuery = intentResult.getDescription();
                if (confidenceQuery == null || confidenceQuery.trim().isEmpty()) {
                    confidenceQuery = text;
                }
                String confidenceContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("confident_purchase", executor.handleConfidentPurchase(phone, confidenceQuery, confidenceContext), false);
                
            case "delegate_request":
                String delegateQuery = intentResult.getDescription();
                if (delegateQuery == null || delegateQuery.trim().isEmpty()) {
                    delegateQuery = text;
                }
                String delegateContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("delegate_request", executor.handleDelegateRequest(phone, delegateQuery, delegateContext), false);
                
            case "contextual_purchase":
                String contextQuery = intentResult.getDescription();
                if (contextQuery == null || contextQuery.trim().isEmpty()) {
                    contextQuery = text;
                }
                String purchaseContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("contextual_purchase", executor.handleContextualPurchase(phone, contextQuery, purchaseContext), false);
                
            case "handle_stock_issue":
                String stockQuery = intentResult.getDescription();
                if (stockQuery == null || stockQuery.trim().isEmpty()) {
                    stockQuery = text;
                }
                String stockContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("handle_stock_issue", executor.handleStockIssue(phone, stockQuery, stockContext), false);
                
            case "handle_price_change":
                String priceQuery = intentResult.getDescription();
                if (priceQuery == null || priceQuery.trim().isEmpty()) {
                    priceQuery = text;
                }
                String priceContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("handle_price_change", executor.handlePriceChange(phone, priceQuery, priceContext), false);
                
            case "system_recovery":
                String systemQuery = intentResult.getDescription();
                if (systemQuery == null || systemQuery.trim().isEmpty()) {
                    systemQuery = text;
                }
                String systemContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("system_recovery", executor.handleSystemRecovery(phone, systemQuery, systemContext), false);
                
            case "handle_conflict":
                String conflictQuery = intentResult.getDescription();
                if (conflictQuery == null || conflictQuery.trim().isEmpty()) {
                    conflictQuery = text;
                }
                String conflictContext = context != null ? context.getLastCartState() : "";
                return new OrchestrationResult("handle_conflict", executor.handleConflict(phone, conflictQuery, conflictContext), false);
                
            case "clear_cart":
                return new OrchestrationResult("clear_cart", executor.clearCart(phone), false);
                
            case "view_cart":
                return new OrchestrationResult("view_cart", executor.showCart(phone), false);
                
            case "order_status":
                return new OrchestrationResult("order_status", executor.getOrderStatus(phone), false);
                
            case "add_to_cart":
                return new OrchestrationResult("add_to_cart", executor.reserveItems(phone, intentResult.getItems()), false);
                
            case "remove_from_cart":
                return new OrchestrationResult("remove_from_cart", executor.removeFromCart(phone, intentResult.getItems()), false);
                
            case "get_products":
                return new OrchestrationResult("get_products", executor.showProducts(), true);
                
            case "help":
                return new OrchestrationResult("help", buildHelpMessage(), false);
                
            case "rate_limit":
                // Rate limit durumunda da kullanıcıya anlamlı yanıt ver
                return new OrchestrationResult("rate_limit", "RATE_LIMIT_FALLBACK", false);
                
            case "unknown":
                return new OrchestrationResult("unknown", intentClassifier.buildDisambiguation(text), false);
                
            case "chat":
                // AI'dan contextual yanıt üretmesini iste
                return new OrchestrationResult("chat", "AI_CONTEXTUAL_RESPONSE", false);
        }
        
        // Config-driven intents üzerinden action çözümlemeyi dene
        java.util.Optional<IntentConfigLoader.IntentDef> cfgIntent = intentConfigLoader.match(text);
        if (cfgIntent.isPresent()) {
            String action = cfgIntent.get().action;
            if (action != null) {
                for (ActionHandler handler : actionHandlers) {
                    if (handler.canHandle(action)) {
                        String handlerResponse = handler.handle(phone, text, context);
                        return new OrchestrationResult(action, handlerResponse, false);
                    }
                }
            }
            if (cfgIntent.get().response != null) {
                return new OrchestrationResult(cfgIntent.get().action, cfgIntent.get().response, false);
            }
        }
        
        // İşlevsel niyetleri işle (mevcut mantık)
        String handlerResponse = handleFunctionalIntent(phone, intentResult, context, text);

        boolean isProductList = "get_products".equals(intent) || "show_products".equals(intent);

        return new OrchestrationResult(intent, handlerResponse, isProductList);
    }
    
    private String handleFunctionalIntent(String phone, IntentClassifier.IntentResult intentResult, CustomerContext context, String text) {
        String intent = intentResult.getIntent();
        
        // Onay gerektiren işlemler
        if (intentResult.isRequiresConfirmation()) {
            if (isAwaiting(phone)) {
                return "Hâlihazırda onayınızı bekliyorum. Lütfen 'evet' ya da 'hayır' yazın.";
            }
            setAwaiting(phone, true);
            return "Siparişi onaylıyor musunuz? (evet / hayır)";
        }
        
        // Doğrudan işlemler
        switch (intent) {
            case "add_to_cart":
                if (intentResult.getItems() == null || intentResult.getItems().isEmpty()) {
                    return executor.showProducts();
                }
                return executor.reserveItems(phone, intentResult.getItems());
                
            case "view_cart":
                return executor.showCart(phone);
                
            case "update_cart":
                return executor.updateCart(phone, intentResult.getItems());
                
            case "remove_from_cart":
                return executor.removeFromCart(phone, intentResult.getItems());
                
            case "clear_cart":
                return executor.clearCart(phone);
                
            case "get_products":
            case "show_products":
                return executor.showProducts();
                
            case "order_status":
                return executor.getOrderStatus(phone);
                
            case "order_history":
                return executor.getOrderHistory(phone);
                
            case "repeat_last_order":
                return executor.repeatLastOrder(phone);
                
            case "show_alternatives":
                String compQuery = intentResult.getDescription();
                if (compQuery == null || compQuery.trim().isEmpty()) {
                    compQuery = text;
                }
                String refContext = context != null ? context.getLastCartState() : "";
                return executor.showAlternatives(phone, compQuery, refContext);
                
            case "clarify_quantity":
                String qtyQuery = intentResult.getDescription();
                if (qtyQuery == null || qtyQuery.trim().isEmpty()) {
                    qtyQuery = text;
                }
                String prodContext = context != null ? context.getLastCartState() : "";
                return executor.clarifyQuantity(phone, qtyQuery, prodContext);
                
            case "emotional_support":
                String emotQuery = intentResult.getDescription();
                if (emotQuery == null || emotQuery.trim().isEmpty()) {
                    emotQuery = text;
                }
                String emotContext = context != null ? context.getLastCartState() : "";
                return executor.handleEmotionalSupport(phone, emotQuery, emotContext);
                
            case "confident_purchase":
                String confQuery = intentResult.getDescription();
                if (confQuery == null || confQuery.trim().isEmpty()) {
                    confQuery = text;
                }
                String confContext = context != null ? context.getLastCartState() : "";
                return executor.handleConfidentPurchase(phone, confQuery, confContext);
                
            case "delegate_request":
                String delQuery = intentResult.getDescription();
                if (delQuery == null || delQuery.trim().isEmpty()) {
                    delQuery = text;
                }
                String delContext = context != null ? context.getLastCartState() : "";
                return executor.handleDelegateRequest(phone, delQuery, delContext);
                
            case "contextual_purchase":
                String ctxQuery = intentResult.getDescription();
                if (ctxQuery == null || ctxQuery.trim().isEmpty()) {
                    ctxQuery = text;
                }
                String ctxContext = context != null ? context.getLastCartState() : "";
                return executor.handleContextualPurchase(phone, ctxQuery, ctxContext);
                
            case "handle_stock_issue":
                String stQuery = intentResult.getDescription();
                if (stQuery == null || stQuery.trim().isEmpty()) {
                    stQuery = text;
                }
                String stContext = context != null ? context.getLastCartState() : "";
                return executor.handleStockIssue(phone, stQuery, stContext);
                
            case "handle_price_change":
                String prQuery = intentResult.getDescription();
                if (prQuery == null || prQuery.trim().isEmpty()) {
                    prQuery = text;
                }
                String prContext = context != null ? context.getLastCartState() : "";
                return executor.handlePriceChange(phone, prQuery, prContext);
                
            case "system_recovery":
                String sysQuery = intentResult.getDescription();
                if (sysQuery == null || sysQuery.trim().isEmpty()) {
                    sysQuery = text;
                }
                String sysContext = context != null ? context.getLastCartState() : "";
                return executor.handleSystemRecovery(phone, sysQuery, sysContext);
                
            case "handle_conflict":
                String conflictQuery2 = intentResult.getDescription();
                if (conflictQuery2 == null || conflictQuery2.trim().isEmpty()) {
                    conflictQuery2 = text;
                }
                String conflictContext2 = context != null ? context.getLastCartState() : "";
                return executor.handleConflict(phone, conflictQuery2, conflictContext2);
                
            case "confirm_order":
                return executor.confirmOrder(phone);
                
            case "cancel_order":
                return executor.cancelOrder(phone);
                
            default:
                return "İsteğinizi anladım ancak bu konuda ne yapacağımı henüz bilemiyorum.";
        }
    }
    
    private String buildHelpMessage() {
        StringBuilder help = new StringBuilder();
        help.append("🤖 MarketAsist - Size nasıl yardımcı olabilirim?\n\n");
        help.append("📦 **Sipariş İşlemleri:**\n");
        help.append("• '2 koli Coca-Cola' - Sepete ürün ekle\n");
        help.append("• 'sepet' - Sepeti görüntüle\n");
        help.append("• 'onayla' - Siparişi onayla\n");
        help.append("• 'iptal' - Siparişi iptal et\n\n");
        help.append("📋 **Bilgi Alma:**\n");
        help.append("• 'ürünler' - Mevcut ürünleri listele\n");
        help.append("• 'sipariş durumu' - Aktif sipariş bilgisi\n");
        help.append("• 'sipariş geçmişi' - Geçmiş siparişler\n\n");
        help.append("💬 **Genel:**\n");
        help.append("• 'başka var mı' - Daha fazla ürün göster\n");
        help.append("• 'yardım' - Bu menüyü göster\n\n");
        help.append("Herhangi bir konuda soru sorabilirsiniz!");
        return help.toString();
    }

    private String confirmKey(String phone) { return "confirm:awaiting:" + phone; }

    private boolean isAwaiting(String phone) {
        try {
            if (redis != null) {
                Object val = redis.opsForValue().get(confirmKey(phone));
                return (val instanceof Boolean) && ((Boolean) val);
            } else {
                Long until = localAwaitingUntilMs.get(phone);
                if (until == null) return false;
                if (until < System.currentTimeMillis()) {
                    localAwaitingUntilMs.remove(phone);
                    return false;
                }
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setAwaiting(String phone, boolean value) {
        try {
            Duration ttl = Duration.ofMinutes(Math.max(1, config.getReservation().getTtlMinutes()));
            if (redis != null) {
                if (value) {
                    // setIfAbsent ile yarışmayı önle, mevcutsa uzatma yapma
                    redis.opsForValue().setIfAbsent(confirmKey(phone), true, ttl);
                } else {
                    redis.delete(confirmKey(phone));
                }
            } else {
                // Geçici tek-instance fallback
                if (value) {
                    localAwaitingUntilMs.put(phone, System.currentTimeMillis() + ttl.toMillis());
                } else {
                    localAwaitingUntilMs.remove(phone);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
