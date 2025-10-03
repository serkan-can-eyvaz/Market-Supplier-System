package com.example.marketsupplier.agent.nlp;

import com.example.marketsupplier.agent.model.NluResult;
import com.example.marketsupplier.service.CustomerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import com.example.marketsupplier.agent.model.Item;

@Component
public class LLMIntentExtractor implements IntentExtractor {

    private static final Logger log = LoggerFactory.getLogger(LLMIntentExtractor.class);
    private final String apiKey;
    private final String modelName;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMIntentExtractor(@Value("${OPENAI_API_KEY:}") String apiKey,
                              @Value("${ai.api.base_url:https://api.openai.com/v1}") String baseUrl,
                              @Value("${ai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.modelName = model;
        
        // Debug logging
        log.info("LLMIntentExtractor initialized with:");
        log.info("- API Key: {}", apiKey != null && !apiKey.isEmpty() ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "EMPTY");
        log.info("- Base URL: {}", baseUrl);
        log.info("- Model: {}", model);
        
        // Validate API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("CRITICAL: OpenAI API key is not configured! LLM features will not work.");
        } else if (!apiKey.startsWith("sk-")) {
            log.warn("WARNING: API key format may be incorrect. Expected format: sk-...");
        } else {
            log.info("API key format validation passed.");
        }
        // Ensure base URL ends with a slash
        this.apiUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public NluResult extract(String text) {
        return extract(text, null);
    }

    @Override
    public NluResult extract(String text, CustomerContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API key is not configured. Returning unhandled_intent.");
            NluResult fallback = new NluResult();
            fallback.setIntent("unhandled_intent");
            fallback.setConfidence(0.0);
            return fallback;
        }

        try {
            String prompt = buildIntentPrompt(text, context);
            
            // Construct the request payload
            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> payload = Map.of(
                    "model", this.modelName,
                    "messages", List.of(message),
                    "temperature", 0.1,
                    "max_tokens", 300
            );
            String requestBody = objectMapper.writeValueAsString(payload);

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.apiUrl))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorMessage = String.format("LLM API request failed with status code %d: %s", response.statusCode(), response.body());
                log.error(errorMessage);
                // Throw a specific exception for rate limiting to be caught separately
                if (response.statusCode() == 429) {
                    throw new RateLimitException("Rate limit exceeded");
                }
                throw new RuntimeException("API request failed with status: " + response.statusCode());
            }

            // Parse the response
            JsonNode rootNode = objectMapper.readTree(response.body());
            String llmResponseJson = rootNode.path("choices").get(0).path("message").path("content").asText();
            
            log.info("DEBUG: Raw LLM response: {}", llmResponseJson);
            
            // Yanıttaki olası markdown formatını temizle
            llmResponseJson = llmResponseJson.replace("```json", "").replace("```", "").trim();
            
            log.info("DEBUG: Cleaned LLM response: {}", llmResponseJson);

            return parseJsonResponse(llmResponseJson);

        } catch (RateLimitException e) {
            log.warn("Rate limit exceeded for OpenAI API.");
            NluResult rateLimitResult = new NluResult();
            rateLimitResult.setIntent("rate_limit_exceeded");
            rateLimitResult.setConfidence(1.0);
            return rateLimitResult;
        } catch (Exception e) {
            log.error("LLM intent extraction failed for text '{}'", text, e);
            // BOTA DÜŞME! Bunun yerine, anlaşılamadığını belirten bir sonuç dön.
            // Bu, AgentOrchestrator tarafından kullanıcıya "Anlayamadım" mesajı olarak çevrilecek.
            NluResult fallbackResult = new NluResult();
            fallbackResult.setIntent("unhandled_intent");
            fallbackResult.setConfidence(0.0);
            return fallbackResult;
        }
    }

    // Custom exception class for clarity
    private static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    private String buildIntentPrompt(String text, CustomerContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are MarketAsist, an intelligent WhatsApp AI ASSISTANT (NOT a bot) for a grocery ordering system.\n");
        prompt.append("You are a helpful, friendly HUMAN-LIKE assistant who understands context and provides personalized service.\n");
        prompt.append("You use natural conversation, remember context, and adapt to customer preferences.\n");
        prompt.append("CRITICAL: You are NOT a rule-based bot - you're an intelligent assistant that understands nuance and context.\n");
        prompt.append("CRITICAL: Always consider the current cart state and conversation context when classifying intent.\n\n");

        // Mod kontrolü - çok katı olmayan ama etkili
        prompt.append("MODE SYSTEM:\n");
        prompt.append("- Only trigger 'show_modes' if user clearly asks about modes ('mod', 'mode', 'modu değiştir', etc.)\n");
        prompt.append("- Current mode determines behavior: ordering_with_ai (default) allows all actions, chat mode only allows conversation\n");
        prompt.append("- Be intelligent about mode context and user intent\n\n");

        prompt.append("INTELLIGENT CART MANAGEMENT:\n");
        prompt.append("- Use AI understanding, not keyword matching\n");
        prompt.append("- Handle Turkish character encoding issues (ürün → urun, ü → u, ¦ptal → iptal)\n");
        prompt.append("- Understand corrupted text: '¦ptal' = 'iptal' = cancel_order\n");
        prompt.append("- 'sepet' alone = 'view_cart' (show current cart contents)\n");
        prompt.append("- 'sepete ne var' / 'sepeti göster' = 'view_cart'\n");
        prompt.append("- 'sepeti boşalt' / 'sepeti temizle' / 'sepeti sil' / 'silmiştik bunları' / 'sepet boşalt sil onları' = 'clear_cart'\n");
        prompt.append("- CRITICAL: 'iptal' alone = 'clear_cart' (clear entire cart, NOT cancel)\n");
        prompt.append("- CRITICAL: 'iptal et', 'iptal ediyorum' = 'clear_cart' (clear entire cart)\n");
        prompt.append("- IMPORTANT: Distinguish 'iptal' (clear_cart) from order cancellation (cancel)\n");
        prompt.append("- SMART CATEGORY LISTING: When user asks for product categories, show ALL products in that category:\n");
        prompt.append("  * 'kola', 'cola', 'gazoz', 'kazoz', 'sarı kola' → 'show_product_category' (list ALL drinks: Coca-Cola, Pepsi, Fanta, Yedigün etc.)\n");
        prompt.append("  * 'su', 'water' → 'show_product_category' (list ALL water brands)\n");
        prompt.append("  * 'çikolata', 'chocolate' → 'show_product_category' (list ALL chocolate products)\n");
        prompt.append("  * CRITICAL: When user asks for a category (not specific product), use 'show_product_category', NOT 'add_to_cart'\n");
        prompt.append("  * Example: 'Kola var mı?' = 'show_product_category', NOT 'add_to_cart'\n");
        prompt.append("- INTELLIGENT EDGE CASE HANDLING (AI-driven, NOT rule-based):\n");
        prompt.append("  * AMBIGUITY RESOLUTION: Use context to understand unclear requests\n");
        prompt.append("    - 'su' in food context = water, in cooking context = could be sesame\n");
        prompt.append("    - 'çay' with drinks = tea beverage, with kitchenware = tea glass\n");
        prompt.append("    - Use conversation history and current cart state for disambiguation\n");
        prompt.append("  * VAGUE QUANTITY INTELLIGENCE: Convert imprecise to precise through conversation\n");
        prompt.append("    - 'biraz süt' → 'clarify' intent: 'Kaç litre süt düşünüyorsunuz? 1 litre mi 2 litre mi?'\n");
        prompt.append("    - 'az şeker' → suggest common quantities based on context\n");
        prompt.append("    - 'çok ekmek' → ask for specific amount while showing available options\n");
        prompt.append("  * SMART TYPO CORRECTION: Understand intent despite spelling errors\n");
        prompt.append("    - 'koala' → understand as 'kola' from context\n");
        prompt.append("    - 'sut' → 'süt', 'ciklet' → 'çiklet', 'urun' → 'ürün'\n");
        prompt.append("    - Turkish character issues: ü→u, ğ→g, ç→c, ş→s, ı→i, ö→o\n");
        prompt.append("  * BRAND vs CATEGORY INTELLIGENCE:\n");
        prompt.append("    - 'Ülker' → show_product_category with brand filter\n");
        prompt.append("    - 'Nestle ürünleri' → category listing for Nestle\n");
        prompt.append("    - Context-aware: if asking about specific product vs browsing\n");
        prompt.append("  * CONFLICT RESOLUTION: Handle contradictory requests intelligently\n");
        prompt.append("    - '3 koli, hayır 2 koli' → understand as quantity update to 2\n");
        prompt.append("    - 'İptal, dur ekle' → ask for clarification: 'Eklemek mi istiyorsunuz?'\n");
        prompt.append("    - Prioritize most recent clear instruction\n");
        prompt.append("  * UNIT DISAMBIGUATION: Intelligent unit inference\n");
        prompt.append("    - '2 su' → clarify: '2 şişe mi yoksa 2 koli mi?'\n");
        prompt.append("    - '1 çikolata' → ask: '1 adet mi 1 paket mi?'\n");
        prompt.append("    - Use product database context for common units\n");
        prompt.append("  * TEMPORAL REFERENCE INTELLIGENCE: Handle time-based references smartly\n");
        prompt.append("    - 'dün aldığım gibi', 'geçen sefer ki', 'önceki gibi' → 'repeat_last_order' (recreate last order)\n");
        prompt.append("    - 'her zamanki siparişim', 'hep aldığım' → 'repeat_last_order' \n");
        prompt.append("    - 'sipariş geçmişi', 'geçmiş siparişler' → 'order_history' (show history)\n");
        prompt.append("    - 'az önce eklediğim', 'şimdi eklediğim' → 'view_cart' (current cart)\n");
        prompt.append("    - 'son siparişim nasıldı' → 'order_history' (show recent orders)\n");
        prompt.append("    - When temporal reference unclear, use 'clarify' with context\n");
        prompt.append("    - AI should distinguish between repeating vs viewing history\n");
        prompt.append("  * COMPARISON & PREFERENCE INTELLIGENCE: Handle comparative requests smartly\n");
        prompt.append("    - 'daha ucuzu var mı?' → 'show_alternatives' with price comparison context\n");
        prompt.append("    - 'en iyisi hangisi?' → 'show_alternatives' with quality/popularity context\n");
        prompt.append("    - 'büyük boy istiyorum' → 'show_alternatives' with size options\n");
        prompt.append("    - 'şekersiz olanından' → 'show_alternatives' with variant options\n");
        prompt.append("    - 'hangi marka daha iyi?' → 'show_alternatives' with brand comparison\n");
        prompt.append("    - Use current cart or conversation context to understand what's being compared\n");
        prompt.append("    - If no reference product, ask for clarification intelligently\n");
        prompt.append("  * QUANTITY INTELLIGENCE: Handle vague/imprecise quantities through AI reasoning\n");
        prompt.append("    - 'bi tane', 'bir tane' → 'clarify_quantity' with unit context from product database\n");
        prompt.append("    - 'yarım kilo', 'çeyrek kilo' → 'clarify_quantity' with weight conversion suggestions\n");
        prompt.append("    - 'biraz süt', 'az şeker' → 'clarify_quantity' with common quantity suggestions\n");
        prompt.append("    - 'çok ekmek', 'bol su' → 'clarify_quantity' with reasonable quantity options\n");
        prompt.append("    - 'düzine', 'çift' → 'clarify_quantity' with number conversion (12, 2)\n");
        prompt.append("    - AI should use product context to suggest appropriate units and quantities\n");
        prompt.append("    - Consider product type, common usage patterns, and available package sizes\n");
        prompt.append("    - When unclear, offer 2-3 specific quantity options rather than open question\n");
        prompt.append("  * EMOTIONAL & SOCIAL INTELLIGENCE: Handle emotional/social contexts with empathy\n");
        prompt.append("    - 'çok üzgünüm, hiçbir şey istemiyorum' → 'emotional_support' with gentle assistance offer\n");
        prompt.append("    - 'artık bıktım', 'sıkıldım' → 'emotional_support' with problem-solving approach\n");
        prompt.append("    - 'kesinlikle bu sefer alacağım' → 'confident_purchase' with supportive guidance\n");
        prompt.append("    - 'annem diyor ki', 'eşim istiyor' → 'delegate_request' with clarification\n");
        prompt.append("    - 'çocuklar için', 'misafir gelecek' → 'contextual_purchase' with appropriate suggestions\n");
        prompt.append("    - Frustrated customers → offer help and solutions, not defensive responses\n");
        prompt.append("    - Uncertain customers → provide reassurance and clear guidance\n");
        prompt.append("    - Multiple people scenarios → clarify who is making the decision\n");
        prompt.append("    - AI should be empathetic, supportive, and solution-oriented\n");
        prompt.append("  * SYSTEM & TECHNICAL INTELLIGENCE: Handle system issues gracefully\n");
        prompt.append("    - Stock changes during order → 'handle_stock_issue' with alternative suggestions\n");
        prompt.append("    - Price changes → 'handle_price_change' with updated pricing info\n");
        prompt.append("    - System errors → 'system_recovery' with helpful explanations\n");
        prompt.append("    - Concurrent operations → 'handle_conflict' with state synchronization\n");
        prompt.append("    - 'ürün yok artık', 'stokta kalmamış' → 'handle_stock_issue' with alternatives\n");
        prompt.append("    - 'fiyat değişmiş', 'daha pahalı olmuş' → 'handle_price_change' with explanation\n");
        prompt.append("    - 'sistem çalışmıyor', 'hata var' → 'system_recovery' with troubleshooting\n");
        prompt.append("    - AI should provide solutions, not just acknowledge problems\n");
        prompt.append("- 'ürünleri görmek istiyorum' / 'ürünler' / 'urunler' / 'ürün listesi' / 'katalog' / 'ne var' = 'get_products'\n");
        prompt.append("- CRITICAL: Any variation of 'ürün/urun' requests should be 'get_products', NOT view_cart\n");
        prompt.append("- CRITICAL: Any 'number + product + action verb' (ekle, al, istiyorum) should be 'add_to_cart', NOT chat\n");
        prompt.append("\n");
        prompt.append("  * ORDER STATUS INTELLIGENCE: Smart order status handling\n");
        prompt.append("    - 'sipariş durumu', 'siparişim nerede', 'sipariş durumum' → 'get_order_status'\n");
        prompt.append("    - 'siparişim onaylandı mı', 'onaylandı mı' → 'get_order_status'\n");
        prompt.append("    - 'teslim edildi mi', 'geldi mi' → 'get_order_status'\n");
        prompt.append("    - 'bekleyen siparişlerim', 'siparişlerim' → 'get_order_status'\n");
        prompt.append("    - AI should understand that order status queries need intelligent filtering:\n");
        prompt.append("      * If there's an approved but not delivered order → show only that with delivery date\n");
        prompt.append("      * If there are pending orders → show only last 5 pending orders\n");
        prompt.append("      * If there are delivered orders → show only the last delivered order\n");
        prompt.append("      * AI should NOT list all orders, but intelligently prioritize based on status\n");
        prompt.append("    - Use context to understand what the customer really wants to know\n");
        prompt.append("    - Provide relevant information without overwhelming the customer\n");
        prompt.append("  * PRODUCT CATEGORY INTELLIGENCE: Smart category-based product discovery\n");
        prompt.append("    - 'kola', 'gazoz', 'sarı kola', 'kazoz', 'soda', 'içecek' → 'show_product_category' with 'içecek'\n");
        prompt.append("    - 'su', 'maden suyu', 'soda' → 'show_product_category' with 'su'\n");
        prompt.append("    - 'çikolata', 'şeker', 'tatlı' → 'show_product_category' with 'çikolata'\n");
        prompt.append("    - 'süt', 'yoğurt', 'peynir' → 'show_product_category' with 'süt ürünleri'\n");
        prompt.append("    - 'ekmek', 'un', 'hamur' → 'show_product_category' with 'ekmek'\n");
        prompt.append("    - AI should understand that category queries need to show ALL products in that category\n");
        prompt.append("    - AI should NOT ask for more details, but show available options for selection\n");
        prompt.append("    - AI should use database context to find all matching products\n");
        prompt.append("    - AI should present products in a clear, selectable format\n");
        prompt.append("- CRITICAL: Any 'number + unit + product' pattern (2 paket cizivis, 3 koli Cola) is ALWAYS 'add_to_cart', NEVER view_cart\n");
        prompt.append("- CRITICAL: Don't confuse product requests with cart viewing - number+product = add_to_cart\n");
        prompt.append("- Be smart about context: if cart just cleared, acknowledge that state\n");
        prompt.append("- Understand fuzzy product names, typos, and natural language\n");
        prompt.append("- Handle contradictions: if user says cart was cleared, respect that\n");
        prompt.append("- Current cart state should influence responses\n");
        prompt.append("- Turkish encoding issues: ▄r³nler = ürünler = 'get_products'\n\n");

        // Context bilgisi ekle - BU ÇOK ÖNEMLİ
        if (context != null && context.isHasActiveCart() && context.getLastCartState() != null && !context.getLastCartState().isBlank()) {
            prompt.append("\n🛒 CRITICAL CART CONTEXT - CONSIDER THIS:\n");
            prompt.append("CURRENT CART STATE: ");
            prompt.append(context.getLastCartState());
            prompt.append("\n");
            prompt.append("REMEMBER: Cart context is for reference only. Still classify 'number + product' as add_to_cart!\n");
        }
        if (context != null && context.getSessionData() != null && context.getSessionData().containsKey("mode")) {
            prompt.append("CURRENT MODE: ").append(String.valueOf(context.getSessionData().get("mode"))).append("\n");
        }

        prompt.append("\nUser message: \"").append(text).append("\"\n");

        prompt.append("\nOutput JSON format:\n");
        prompt.append("{\"intent\":\"<intent_name>\", \"items\":[{\"raw\":\"<original_text>\", \"product_query\":\"<clean_product_name>\", \"product_id\":null, \"qty\":<number>, \"unit\":\"<koli|kg|adet>\"}], \"confidence\":<0.0_to_1.0>}\n");

        prompt.append("\nAVAILABLE INTENTS:\n");
        prompt.append("- `show_modes`: User explicitly asks about available modes\n");
        prompt.append("- `add_to_cart`: User wants to add items with specific quantities (CRITICAL: includes 'ekle', 'alabilir', 'istiyorum', 'alsak' verbs)\n");
        prompt.append("- `update_cart`: User wants to modify existing cart item quantities\n");
        prompt.append("- `remove_from_cart`: User wants to remove specific items from cart\n");
        prompt.append("- `clear_cart`: User wants to empty entire cart\n");
        prompt.append("- `view_cart`: User explicitly wants to see current cart contents\n");
        prompt.append("- `get_products`: User wants to see available products\n");
        prompt.append("- `navigate`: User wants to see more/next products ('başka var mı', 'daha fazla', 'devam')\n");
        prompt.append("- `confirm_order`: User wants to confirm/place order\n");
        prompt.append("- `cancel_order`: User cancels order or rejects confirmation\n");
        prompt.append("- `get_order_status`: User asks about current order status\n");
        prompt.append("- `get_order_history`: User wants order history\n");
        prompt.append("- `chat`: General conversation, greetings, questions, vague requests\n\n");

        prompt.append("NATURAL LANGUAGE INTELLIGENCE:\n");
        prompt.append("- Understand context, state, and user intent holistically\n");
        prompt.append("- Handle typos, variations, and natural speech patterns\n");
        prompt.append("- For vague requests ('meyve lazım', 'temizlik ürünü'), use 'chat'\n");
        prompt.append("- For greetings mixed with actions, prioritize natural conversation\n");
        prompt.append("- Understand negations and confirmations contextually\n");
        prompt.append("- Be smart about quantities, units, and product references\n");
        prompt.append("- Consider conversation flow and user behavior patterns\n\n");
        
        prompt.append("OUTPUT FORMAT: Return ONLY the JSON object, nothing else. No text before or after JSON.\n");
        prompt.append("CRITICAL: Your response must be EXACTLY in this format: {\"intent\":\"...\", \"items\":[...], \"confidence\":0.95}\n");
        prompt.append("Do NOT include any explanations, greetings, or other text. ONLY JSON.\n");

        // Few-shot örnekleri ekle
        prompt.append("\nExamples (Turkish):\n");
        prompt.append("USER: mod\n");
        prompt.append("JSON: {\"intent\":\"show_modes\", \"items\":[], \"confidence\":1.0}\n\n");
        
        prompt.append("USER: Sepeti boşalt\n");
        prompt.append("JSON: {\"intent\":\"clear_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepeti boşaltalım\n");
        prompt.append("JSON: {\"intent\":\"clear_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepette ne var\n");
        prompt.append("JSON: {\"intent\":\"view_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepeti göster\n");
        prompt.append("JSON: {\"intent\":\"view_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepeti görmek istiyorum\n");
        prompt.append("JSON: {\"intent\":\"view_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepetim nasıl\n");
        prompt.append("JSON: {\"intent\":\"view_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Ürünleri görmek istiyorum\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: urunler\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Ürünler\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: ürünler\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: ÜRÜNLER\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: ▄r³nler\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Daha fazla ürün\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Farklı ürünleri görmek istiyorum\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Ürünleri görebilir miyim\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: urunleri goster\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: ³r³nleri goster\n");
        prompt.append("JSON: {\"intent\":\"get_products\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Silmiştik bunları\n");
        prompt.append("JSON: {\"intent\":\"clear_cart\", \"items\":[], \"confidence\":0.90}\n\n");
        
        prompt.append("USER: onayla\n");
        prompt.append("JSON: {\"intent\":\"confirm_order\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: iptal\n");
        prompt.append("JSON: {\"intent\":\"cancel_order\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 2 koli Cola\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"2 koli Cola\", \"product_query\":\"Cola\", \"product_id\":null, \"qty\":2, \"unit\":\"koli\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepet\n");
        prompt.append("JSON: {\"intent\":\"view_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Albeniyi sil istemiyorum onh\n");
        prompt.append("JSON: {\"intent\":\"chat\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: istemiyorum\n");
        prompt.append("JSON: {\"intent\":\"chat\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Başka var mı\n");
        prompt.append("JSON: {\"intent\":\"navigate\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Baska var mi\n");
        prompt.append("JSON: {\"intent\":\"navigate\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: başka var mı\n");
        prompt.append("JSON: {\"intent\":\"navigate\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: daha fazla ürün\n");
        prompt.append("JSON: {\"intent\":\"navigate\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: devam\n");
        prompt.append("JSON: {\"intent\":\"navigate\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Tümünü sepetten çıkar\n");
        prompt.append("JSON: {\"intent\":\"clear_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: Sepetteki ürünleri silmek istiyorum\n");
        prompt.append("JSON: {\"intent\":\"clear_cart\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: iptal\n");
        prompt.append("JSON: {\"intent\":\"cancel_order\", \"items\":[], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: ¦ptal\n");
        prompt.append("JSON: {\"intent\":\"cancel_order\", \"items\":[], \"confidence\":0.95}\n\n");
        
        // Add to cart examples
        prompt.append("USER: 3 paket Albeni\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"3 paket Albeni\", \"product_query\":\"Albeni\", \"product_id\":null, \"qty\":3, \"unit\":\"paket\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 3 koli Coca-Cola ekleyelim\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"3 koli Coca-Cola\", \"product_query\":\"Coca-Cola\", \"product_id\":null, \"qty\":3, \"unit\":\"koli\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 2 paket çikolata alabilir miyiz\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"2 paket çikolata\", \"product_query\":\"çikolata\", \"product_id\":null, \"qty\":2, \"unit\":\"paket\"}], \"confidence\":0.90}\n\n");
        
        prompt.append("USER: 5 koli domates istiyorum\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"5 koli domates\", \"product_query\":\"domates\", \"product_id\":null, \"qty\":5, \"unit\":\"koli\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 2 paket Albeni ekleyebilir miyiz\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"2 paket Albeni\", \"product_query\":\"Albeni\", \"product_id\":null, \"qty\":2, \"unit\":\"paket\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 4 adet su alalım\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"4 adet su\", \"product_query\":\"su\", \"product_id\":null, \"qty\":4, \"unit\":\"adet\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 2 paket cizivis\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"2 paket cizivis\", \"product_query\":\"cizivis\", \"product_id\":null, \"qty\":2, \"unit\":\"paket\"}], \"confidence\":0.95}\n\n");
        
        prompt.append("USER: 1 paket çiziviç\n");
        prompt.append("JSON: {\"intent\":\"add_to_cart\", \"items\":[{\"raw\":\"1 paket çiziviç\", \"product_query\":\"çiziviç\", \"product_id\":null, \"qty\":1, \"unit\":\"paket\"}], \"confidence\":0.95}\n\n");
        
        return prompt.toString();
    }

    private NluResult parseJsonResponse(String jsonResponse) {
        try {
            // Clean response from any unwanted prefixes or content before JSON
            String cleanJson = jsonResponse.trim();
            
            // FIXED: Find the FIRST occurrence of the complete JSON object
            int jsonStart = cleanJson.indexOf("JSON:");
            if (jsonStart != -1) {
                cleanJson = cleanJson.substring(jsonStart + 5).trim();
            }
            
            // If no "JSON:" prefix found, look for the FIRST occurrence of JSON object
            if (jsonStart == -1) {
                jsonStart = cleanJson.indexOf("{");
                if (jsonStart != -1) {
                    cleanJson = cleanJson.substring(jsonStart);
                }
            }
            
            // Additional cleanup for any remaining prefixes
            if (cleanJson.startsWith("USER:")) {
                jsonStart = cleanJson.indexOf("{");
                if (jsonStart != -1) {
                    cleanJson = cleanJson.substring(jsonStart);
                }
            }
            
            // ObjectMapper kullanarak JSON ayrıştırma + güvenli normalizasyon
            NluResult result = objectMapper.readValue(cleanJson, NluResult.class);
            if (result.getIntent() == null) {
                result.setIntent("unhandled_intent");
            }
            if (result.getItems() == null) result.setItems(new ArrayList<>());
            // items normalizasyonu
            for (Item it : result.getItems()) {
                if (it == null) continue;
                if (it.getQty() < 0) it.setQty(0);
                it.setUnit(normalizeUnit(it.getUnit()));
                if (it.getProductQuery() == null) it.setProductQuery("");
            }
            if (result.getConfidence() < 0 || result.getConfidence() > 1) result.setConfidence(0.0);
            return result;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to parse LLM JSON response: '{}' - Error: {}", jsonResponse, e.getMessage(), e);
            log.error("CRITICAL: JSON that failed: {}", jsonResponse);
            // JSON parse edilemezse, bu da bir unhandled intent durumudur.
            NluResult fallbackResult = new NluResult();
            fallbackResult.setIntent("unhandled_intent");
            fallbackResult.setConfidence(0.0);
            return fallbackResult;
        }
    }

    private String normalizeUnit(String unit) {
        if (unit == null) return null;
        String u = unit.toLowerCase(Locale.ROOT).trim();
        if (u.equals("kilo")) u = "kg";
        if (u.equals("adet") || u.equals("kg") || u.equals("koli")) return u;
        return null;
    }
}
