package com.example.marketsupplier.agent.nlp;

import com.example.marketsupplier.service.CustomerContext;
import com.example.marketsupplier.agent.model.NluResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.example.marketsupplier.agent.metrics.AgentMetrics;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifier.class);

    @Autowired
    private LLMIntentExtractor llmExtractor;

    // Rule-based çıkarım kaldırıldı; yalnızca LLM kullanılır

    @Autowired
    private com.example.marketsupplier.agent.config.AgentConfig config;

    @Autowired(required = false)
    private AgentMetrics metrics;

    // Özel anahtar kelime/regex tabanlı niyet tespiti kaldırıldı

    public IntentResult classify(String text, CustomerContext context) {
        if (text == null || text.trim().isEmpty()) {
            return IntentResult.unknown("Boş mesaj alındı");
        }

        String normalized = text.toLowerCase().trim();
        String norm = normalizeAscii(normalized);

        // LLM tabanlı sınıflandırma
        if (config.getLlm().isEnabled()) {
            try {
                NluResult nluResult = llmExtractor.extract(text, context);
                IntentResult ir = convertNluToIntent(nluResult, text);
                try { if (metrics != null) metrics.incIntent(ir.getIntent()); } catch (Exception ignored) {}
                return ir;
            } catch (Exception e) {
                log.warn("LLM intent extraction failed", e);
                return IntentResult.chat("Genel sohbet");
            }
        }
        // LLM kapalıysa koruyucu olarak sohbet moduna düş
        return IntentResult.chat("Genel sohbet");
    }

    // Özel durum ve anahtar kelime tabanlı denetimler kaldırıldı; LLM karar verir

    // Belirsiz durumda statik öneri üretmeyelim; sohbet moduna düşeceğiz
    public String buildDisambiguation(String original) {
        return "Daha iyi yardımcı olabilmem için biraz daha detay verir misiniz?";
    }

    private boolean isGeneralChat(String text) {
        // Çok kısa mesajlar veya selamlaşma
        if (text.length() < 3) return true;
        String[] chatPatterns = {"merhaba", "selam", "hi", "hello", "nasılsın", "nasilsin", "teşekkür", "tesekkur"};
        return Arrays.stream(chatPatterns).anyMatch(text::contains);
    }

    // Kelime eşleşmesine dayalı savunmalar kaldırıldı

    private String normalizeAscii(String s) {
        if (s == null) return "";
        String x = s
            .replace('ş','s').replace('Ş','S')
            .replace('ı','i').replace('İ','I')
            .replace('ç','c').replace('Ç','C')
            .replace('ğ','g').replace('Ğ','G')
            .replace('ö','o').replace('Ö','O')
            .replace('ü','u').replace('Ü','U');
        // Bazı bozulmuş karakterleri temizle
        x = x.replaceAll("[^a-z0-9\u0020]", " ");
        x = x.replaceAll("\\s+", " ").trim();
        return x;
    }

    private IntentResult convertNluToIntent(NluResult nluResult, String originalText) {
        if (nluResult == null) {
            log.error("CRITICAL: NluResult is null!");
            return IntentResult.unknown("Sınıflandırma başarısız");
        }

        String intent = nluResult.getIntent();
        double confidence = nluResult.getConfidence();
        double threshold = config.getNlu().getConfidenceThreshold();
        
        log.info("DEBUG: Intent='{}', Confidence={}, Threshold={}", intent, confidence, threshold);

        if (confidence < threshold) {
            log.warn("CRITICAL: Confidence too low! Intent='{}', Confidence={}, Threshold={}", intent, confidence, threshold);
            return IntentResult.unknown("Güven düşük: " + originalText);
        }

        switch (intent) {
            case "add_to_cart":
                return IntentResult.addToCart("Sepete ekleme", nluResult.getItems());
            case "view_cart":
                return IntentResult.viewCart("Sepeti görüntüle");
            case "update_cart":
                return IntentResult.updateCart("Sepeti güncelle", nluResult.getItems());
            case "remove_from_cart":
                return IntentResult.removeFromCart("Sepetten çıkar", nluResult.getItems());
            case "clear_cart":
                return IntentResult.clearCart("Sepeti temizle");
            case "get_products":
                return IntentResult.showProducts("Ürünleri göster");
            case "confirm_order":
                return IntentResult.confirmOrder("Siparişi onayla");
            case "cancel_order":
                return IntentResult.cancelOrder("Siparişi iptal et");
            case "get_order_status":
                return IntentResult.orderStatus("Sipariş durumu");
            case "get_order_history":
                return IntentResult.orderHistory("Sipariş geçmişi");
            case "chat":
                return IntentResult.chat("Genel sohbet");
            case "rate_limit_exceeded":
                return IntentResult.rateLimit("Rate limit aşıldı");
            case "show_modes":
                return IntentResult.showModes("Mod seçeneklerini göster");
            default:
                return IntentResult.unknown("Bilinmeyen intent: " + intent);
        }
    }

    public static class IntentResult {
        private final String intent;
        private final String description;
        private final List<com.example.marketsupplier.agent.model.Item> items;
        private final double confidence;
        private final boolean requiresConfirmation;

        private IntentResult(String intent, String description, List<com.example.marketsupplier.agent.model.Item> items, 
                           double confidence, boolean requiresConfirmation) {
            this.intent = intent;
            this.description = description;
            this.items = items != null ? items : new ArrayList<>();
            this.confidence = confidence;
            this.requiresConfirmation = requiresConfirmation;
        }

        // Factory methods
        public static IntentResult addToCart(String desc, List<com.example.marketsupplier.agent.model.Item> items) {
            return new IntentResult("add_to_cart", desc, items, 0.9, false);
        }

        public static IntentResult viewCart(String desc) {
            return new IntentResult("view_cart", desc, null, 0.9, false);
        }

        public static IntentResult updateCart(String desc, List<com.example.marketsupplier.agent.model.Item> items) {
            return new IntentResult("update_cart", desc, items, 0.9, false);
        }

        public static IntentResult removeFromCart(String desc, List<com.example.marketsupplier.agent.model.Item> items) {
            return new IntentResult("remove_from_cart", desc, items, 0.9, false);
        }

        public static IntentResult clearCart(String desc) {
            return new IntentResult("clear_cart", desc, null, 0.9, false);
        }

        public static IntentResult showProducts(String desc) {
            return new IntentResult("show_products", desc, null, 0.9, false);
        }

        public static IntentResult confirmOrder(String desc) {
            return new IntentResult("confirm_order", desc, null, 0.9, true);
        }

        public static IntentResult cancelOrder(String desc) {
            return new IntentResult("cancel_order", desc, null, 0.9, false);
        }

        public static IntentResult orderStatus(String desc) {
            return new IntentResult("order_status", desc, null, 0.9, false);
        }

        public static IntentResult orderHistory(String desc) {
            return new IntentResult("order_history", desc, null, 0.9, false);
        }

        public static IntentResult chat(String desc) {
            return new IntentResult("chat", desc, null, 0.8, false);
        }

        public static IntentResult confirm(String desc) {
            return new IntentResult("confirm", desc, null, 0.9, false);
        }

        public static IntentResult cancel(String desc) {
            return new IntentResult("cancel", desc, null, 0.9, false);
        }

        public static IntentResult clarify(String desc) {
            return new IntentResult("clarify", desc, null, 0.9, false);
        }

        public static IntentResult navigate(String desc) {
            return new IntentResult("navigate", desc, null, 0.9, false);
        }

        public static IntentResult help(String desc) {
            return new IntentResult("help", desc, null, 0.9, false);
        }

        public static IntentResult rateLimit(String desc) {
            return new IntentResult("rate_limit", desc, null, 0.9, false);
        }
        
        public static IntentResult showModes(String desc) {
            return new IntentResult("show_modes", desc, null, 0.9, false);
        }
        
        public static IntentResult unknown(String desc) {
            return new IntentResult("unknown", desc, null, 0.0, false);
        }

        // Getters
        public String getIntent() { return intent; }
        public String getDescription() { return description; }
        public List<com.example.marketsupplier.agent.model.Item> getItems() { return items; }
        public double getConfidence() { return confidence; }
        public boolean isRequiresConfirmation() { return requiresConfirmation; }
    }
}
