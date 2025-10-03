package com.example.marketsupplier.agent.executor;

import com.example.marketsupplier.agent.model.NluResult;
import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.OrderService;
import com.example.marketsupplier.service.ProductService;

import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.agent.model.Item;
import com.example.marketsupplier.agent.util.StringNormalizer;
import java.util.stream.Collectors;
import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.OrderItem;
import com.example.marketsupplier.entity.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.Delivery;
import com.example.marketsupplier.service.OrderPdfService;
import com.example.marketsupplier.service.WhatsAppService;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ActionExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final MarketService marketService;
    private final com.example.marketsupplier.agent.metrics.AgentMetrics metrics;
    private final com.example.marketsupplier.agent.audit.AgentAuditService audit;
    private final StringNormalizer stringNormalizer;
    private final OrderPdfService orderPdfService;
    private final WhatsAppService whatsAppService;

    @Autowired
    public ActionExecutor(ProductService productService, CartService cartService, OrderService orderService,
                          MarketService marketService, Optional<com.example.marketsupplier.agent.metrics.AgentMetrics> metrics, 
                          Optional<com.example.marketsupplier.agent.audit.AgentAuditService> audit, StringNormalizer stringNormalizer,
                          OrderPdfService orderPdfService, WhatsAppService whatsAppService) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.marketService = marketService;
        this.metrics = metrics.orElse(null);
        this.audit = audit.orElse(null);
        this.stringNormalizer = stringNormalizer;
        this.orderPdfService = orderPdfService;
        this.whatsAppService = whatsAppService;
    }

    public String getOrderStatus(String phone) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Numaranız sisteme kayıtlı değil.";
        }

        // AI-driven akıllı sipariş durumu sorgulama
        return orderService.getSmartOrderStatus(market.getId());
    }

    public String getOrderHistory(String phone) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Numaranız sisteme kayıtlı değil.";
        }
        
        List<Order> completedOrders = orderService.findAllCompletedOrdersByMarket(market.getId());
        if(completedOrders.isEmpty()){
            return "Daha önce tamamlanmış bir siparişiniz bulunmuyor. İlk siparişinizi vermek ister misiniz?";
        }
        
        // AI-driven smart order history response
        return generateSmartOrderHistoryResponse(completedOrders, phone);
    }
    
    private String generateSmartOrderHistoryResponse(List<Order> orders, String phone) {
        StringBuilder response = new StringBuilder();
        response.append("📋 **Geçmiş Siparişleriniz:**\n\n");
        
        // Show last 3 orders with details
        int limit = Math.min(3, orders.size());
        for (int i = 0; i < limit; i++) {
            Order order = orders.get(i);
            List<OrderItem> items = orderService.getOrderItems(order.getId());
            
            response.append("🗓️ **").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("tr")))).append("**\n");
            response.append("💰 Toplam: ").append(order.getTotalPrice()).append(" TL\n");
            response.append("📦 Ürünler: ");
            
            // Show first few items
            int itemLimit = Math.min(3, items.size());
            for (int j = 0; j < itemLimit; j++) {
                OrderItem item = items.get(j);
                response.append(item.getProductName());
                if (j < itemLimit - 1) response.append(", ");
            }
            if (items.size() > 3) {
                response.append(" ve ").append(items.size() - 3).append(" ürün daha");
            }
            response.append("\n\n");
        }
        
        if (orders.size() > 3) {
            response.append("📄 Tüm geçmişinizi PDF olarak göndermek ister misiniz?\n\n");
        }
        
        // AI-driven suggestions based on order history
        response.append("💡 **Öneriler:**\n");
        response.append("- 'Dün aldığım gibi' diyerek son siparişinizi tekrarlayabilirsiniz\n");
        response.append("- 'Geçen sefer ki ürünleri' diyerek önceki ürünleri görebilirsiniz\n");
        response.append("- Belirli bir tarihten bahsederek o siparişi bulabiliriz\n");
        
        return response.toString();
    }
    
    public String repeatLastOrder(String phone) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Numaranız sisteme kayıtlı değil.";
        }
        
        List<Order> completedOrders = orderService.findAllCompletedOrdersByMarket(market.getId());
        if(completedOrders.isEmpty()){
            return "Daha önce tamamlanmış bir siparişiniz bulunmuyor.";
        }
        
        Order lastOrder = completedOrders.get(0);
        List<OrderItem> lastItems = orderService.getOrderItems(lastOrder.getId());
        
        StringBuilder response = new StringBuilder();
        response.append("🔄 **Son Siparişinizi Tekrarlıyorum:**\n\n");
        
        // Convert order items to cart items format
        List<Map<String, Object>> cartItems = new ArrayList<>();
        for (OrderItem item : lastItems) {
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productName", item.getProductName());
            cartItem.put("quantity", item.getQuantity());
            cartItem.put("unit", item.getUnit());
            cartItem.put("price", item.getPrice());
            cartItems.add(cartItem);
            
            response.append("- ").append(item.getProductName())
                    .append(" x").append(item.getQuantity()).append(" ").append(item.getUnit())
                    .append(" - ").append(item.getPrice()).append(" TL\n");
        }
        
        // Add to cart
        cartService.replaceItems(market.getId(), cartItems);
        
        response.append("\n💰 **Toplam:** ").append(lastOrder.getTotalPrice()).append(" TL\n");
        response.append("✅ Ürünler sepetinize eklendi. Onaylamak ister misiniz?");
        
        return response.toString();
    }

    public String showAlternatives(String phone, String comparisonQuery, String referenceContext) {
        log.info("showAlternatives() called with query: {}, context: {}", comparisonQuery, referenceContext);
        
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Numaranız sisteme kayıtlı değil.";
        }
        
        List<com.example.marketsupplier.entity.Product> allProducts = productService.getAllActiveProducts();
        if (allProducts.isEmpty()) {
            return "Şu anda aktif ürün bulunmuyor.";
        }
        
        // AI-driven context analysis
        String referenceProduct = extractReferenceProduct(comparisonQuery, referenceContext);
        String comparisonType = extractComparisonType(comparisonQuery);
        
        List<com.example.marketsupplier.entity.Product> alternatives = findAlternativeProducts(
            allProducts, referenceProduct, comparisonType
        );
        
        if (alternatives.isEmpty()) {
            return generateNoAlternativesResponse(comparisonQuery, referenceProduct);
        }
        
        return generateAlternativesResponse(alternatives, comparisonType, referenceProduct);
    }
    
    private String extractReferenceProduct(String query, String context) {
        // AI-driven context analysis to find what product is being referenced
        String normalized = query.toLowerCase().trim();
        
        // Check if there's a product mentioned in the query
        if (normalized.contains("coca-cola") || normalized.contains("kola")) return "coca-cola";
        if (normalized.contains("pepsi")) return "pepsi";
        if (normalized.contains("fanta")) return "fanta";
        if (normalized.contains("su")) return "su";
        if (normalized.contains("çikolata") || normalized.contains("albeni")) return "çikolata";
        
        // If no explicit product, try to infer from context
        if (context != null && !context.trim().isEmpty()) {
            String contextNorm = context.toLowerCase();
            if (contextNorm.contains("coca-cola") || contextNorm.contains("kola")) return "coca-cola";
            if (contextNorm.contains("pepsi")) return "pepsi";
            if (contextNorm.contains("fanta")) return "fanta";
        }
        
        return null; // No clear reference found
    }
    
    private String extractComparisonType(String query) {
        String normalized = query.toLowerCase().trim();
        
        if (normalized.contains("ucuz") || normalized.contains("fiyat")) return "price";
        if (normalized.contains("iyi") || normalized.contains("kalite") || normalized.contains("en")) return "quality";
        if (normalized.contains("büyük") || normalized.contains("boy") || normalized.contains("size")) return "size";
        if (normalized.contains("şekersiz") || normalized.contains("light") || normalized.contains("diet")) return "variant";
        if (normalized.contains("marka") || normalized.contains("brand")) return "brand";
        
        return "general"; // Default comparison type
    }
    
    private List<com.example.marketsupplier.entity.Product> findAlternativeProducts(
            List<com.example.marketsupplier.entity.Product> allProducts, 
            String referenceProduct, 
            String comparisonType) {
        
        List<com.example.marketsupplier.entity.Product> alternatives = new ArrayList<>();
        
        for (com.example.marketsupplier.entity.Product product : allProducts) {
            String productName = product.getName().toLowerCase();
            
            // AI-driven alternative matching based on reference and comparison type
            boolean isAlternative = false;
            
            if (referenceProduct != null) {
                // Find products in same category as reference
                if (referenceProduct.contains("kola") || referenceProduct.contains("coca-cola")) {
                    isAlternative = isDrinkProduct(productName);
                } else if (referenceProduct.contains("su")) {
                    isAlternative = isWaterProduct(productName);
                } else if (referenceProduct.contains("çikolata")) {
                    isAlternative = isChocolateProduct(productName);
                }
            } else {
                // No reference product, show popular alternatives based on comparison type
                if ("price".equals(comparisonType)) {
                    isAlternative = product.getPrice().doubleValue() < 50.0; // Budget options
                } else if ("quality".equals(comparisonType)) {
                    isAlternative = product.getPrice().doubleValue() > 20.0; // Premium options
                }
            }
            
            if (isAlternative) {
                alternatives.add(product);
            }
        }
        
        // Sort based on comparison type
        if ("price".equals(comparisonType)) {
            alternatives.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
        } else if ("quality".equals(comparisonType)) {
            alternatives.sort((a, b) -> b.getPrice().compareTo(a.getPrice())); // Higher price = better quality assumption
        }
        
        return alternatives;
    }
    
    private String generateNoAlternativesResponse(String query, String referenceProduct) {
        if (referenceProduct == null) {
            return "Hangi ürün için alternatif arıyorsunuz? Lütfen belirtir misiniz?";
        }
        return String.format("Maalesef %s için alternatif bulunamadı. Başka bir kategori önerebilirim.", referenceProduct);
    }
    
    private String generateAlternativesResponse(List<com.example.marketsupplier.entity.Product> alternatives, 
                                              String comparisonType, String referenceProduct) {
        StringBuilder response = new StringBuilder();
        
        String title = getComparisonTitle(comparisonType, referenceProduct);
        response.append("🔍 **").append(title).append(":**\n\n");
        
        int limit = Math.min(5, alternatives.size()); // Show max 5 alternatives
        for (int i = 0; i < limit; i++) {
            com.example.marketsupplier.entity.Product product = alternatives.get(i);
            response.append("- ").append(product.getName())
                    .append(" - ").append(product.getPrice()).append(" TL/").append(product.getUnit());
            
            // Add contextual info based on comparison type
            if ("price".equals(comparisonType) && i == 0) {
                response.append(" ⭐ (En ucuz)");
            } else if ("quality".equals(comparisonType) && i == 0) {
                response.append(" ⭐ (Premium)");
            }
            response.append("\n");
        }
        
        if (alternatives.size() > 5) {
            response.append("... ve ").append(alternatives.size() - 5).append(" seçenek daha\n");
        }
        
        response.append("\nHangisini sepete eklemek istersiniz? Miktarını belirtip yazabilirsiniz.");
        
        return response.toString();
    }
    
    private String getComparisonTitle(String comparisonType, String referenceProduct) {
        String productContext = referenceProduct != null ? " (" + referenceProduct + ")" : "";
        
        switch (comparisonType) {
            case "price": return "Uygun Fiyatlı Seçenekler" + productContext;
            case "quality": return "Kaliteli Seçenekler" + productContext;
            case "size": return "Farklı Boy Seçenekleri" + productContext;
            case "variant": return "Varyant Seçenekleri" + productContext;
            case "brand": return "Marka Seçenekleri" + productContext;
            default: return "Alternatif Ürünler" + productContext;
        }
    }

    public String clarifyQuantity(String phone, String quantityQuery, String productContext) {
        log.info("clarifyQuantity() called with query: {}, product context: {}", quantityQuery, productContext);
        
        // AI-driven quantity clarification based on context
        String extractedProduct = extractProductFromContext(quantityQuery, productContext);
        String quantityType = analyzeQuantityType(quantityQuery);
        
        return generateQuantityClarification(extractedProduct, quantityType, quantityQuery);
    }
    
    private String extractProductFromContext(String query, String context) {
        String combined = (query + " " + (context != null ? context : "")).toLowerCase();
        
        // AI-driven product extraction from context
        if (combined.contains("süt") || combined.contains("milk")) return "süt";
        if (combined.contains("şeker") || combined.contains("sugar")) return "şeker";
        if (combined.contains("ekmek") || combined.contains("bread")) return "ekmek";
        if (combined.contains("su") || combined.contains("water")) return "su";
        if (combined.contains("çikolata") || combined.contains("chocolate")) return "çikolata";
        if (combined.contains("kola") || combined.contains("cola")) return "kola";
        if (combined.contains("peynir") || combined.contains("cheese")) return "peynir";
        if (combined.contains("yoğurt") || combined.contains("yogurt")) return "yoğurt";
        
        return null; // No clear product identified
    }
    
    private String analyzeQuantityType(String query) {
        String normalized = query.toLowerCase().trim();
        
        if (normalized.contains("bi tane") || normalized.contains("bir tane")) return "single_unit";
        if (normalized.contains("yarım") || normalized.contains("0.5")) return "half_unit";
        if (normalized.contains("çeyrek") || normalized.contains("0.25")) return "quarter_unit";
        if (normalized.contains("düzine")) return "dozen";
        if (normalized.contains("çift")) return "pair";
        if (normalized.contains("biraz") || normalized.contains("az")) return "small_amount";
        if (normalized.contains("çok") || normalized.contains("bol")) return "large_amount";
        
        return "unspecified"; // General vague quantity
    }
    
    private String generateQuantityClarification(String product, String quantityType, String originalQuery) {
        StringBuilder response = new StringBuilder();
        
        if (product == null) {
            response.append("Hangi ürün için miktar belirtmek istiyorsunuz? ");
            response.append("Lütfen ürün adını da belirtir misiniz?");
            return response.toString();
        }
        
        // AI-driven contextual quantity suggestions
        response.append("🤔 **Miktar Netleştirme:**\n\n");
        
        switch (quantityType) {
            case "single_unit":
                response.append(generateSingleUnitOptions(product));
                break;
            case "half_unit":
                response.append(generateHalfUnitOptions(product));
                break;
            case "quarter_unit":
                response.append(generateQuarterUnitOptions(product));
                break;
            case "dozen":
                response.append(String.format("'Düzine %s' = 12 adet %s demek. Doğru mu?\n", product, product));
                response.append("Sepete 12 adet ekleyeyim mi?");
                break;
            case "pair":
                response.append(String.format("'Çift %s' = 2 adet %s demek. Doğru mu?\n", product, product));
                response.append("Sepete 2 adet ekleyeyim mi?");
                break;
            case "small_amount":
                response.append(generateSmallAmountOptions(product));
                break;
            case "large_amount":
                response.append(generateLargeAmountOptions(product));
                break;
            default:
                response.append(generateGeneralQuantityOptions(product));
                break;
        }
        
        return response.toString();
    }
    
    private String generateSingleUnitOptions(String product) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("'Bi tane %s' için hangi birimi kastediyorsunuz?\n\n", product));
        
        // Context-aware unit suggestions based on product type
        if (product.contains("süt")) {
            response.append("• 1 litre süt\n• 1 şişe süt\n• 1 paket süt");
        } else if (product.contains("çikolata")) {
            response.append("• 1 adet çikolata\n• 1 paket çikolata\n• 1 kutu çikolata");
        } else if (product.contains("su")) {
            response.append("• 1 şişe su\n• 1 litre su\n• 1 koli su (6'lı)");
        } else if (product.contains("ekmek")) {
            response.append("• 1 somun ekmek\n• 1 paket ekmek\n• 1 adet ekmek");
        } else {
            response.append("• 1 adet ").append(product).append("\n");
            response.append("• 1 paket ").append(product).append("\n");
            response.append("• 1 kutu ").append(product);
        }
        
        response.append("\n\nHangisini kastettiniz?");
        return response.toString();
    }
    
    private String generateHalfUnitOptions(String product) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("'Yarım %s' için şu seçenekler var:\n\n", product));
        
        if (product.contains("kilo")) {
            response.append("• 500g paket (yarım kilo)\n");
            response.append("• 2 adet 250g paket");
        } else if (product.contains("litre")) {
            response.append("• 500ml şişe (yarım litre)\n");
            response.append("• 2 adet 250ml");
        } else {
            response.append("Yarım birim için uygun paket boyutu önerebilirim.\n");
            response.append("Hangi boyutta paket istiyorsunuz?");
        }
        
        return response.toString();
    }
    
    private String generateQuarterUnitOptions(String product) {
        return String.format("'Çeyrek %s' için 250g/250ml paket uygun olur mu?\nYoksa farklı bir miktar mı istiyorsunuz?", product);
    }
    
    private String generateSmallAmountOptions(String product) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("'%s için az miktar' derken şunlardan hangisini kastediyorsunuz?\n\n", product));
        
        if (product.contains("süt")) {
            response.append("• 1 litre süt\n• 500ml süt\n• 2 litre süt");
        } else if (product.contains("şeker")) {
            response.append("• 1 paket şeker (1kg)\n• 500g şeker\n• 2 paket şeker");
        } else {
            response.append("• 1 adet ").append(product).append("\n");
            response.append("• 2 adet ").append(product).append("\n");
            response.append("• 1 paket ").append(product);
        }
        
        return response.toString();
    }
    
    private String generateLargeAmountOptions(String product) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("'Çok %s' derken ne kadar düşünüyorsunuz?\n\n", product));
        
        if (product.contains("ekmek")) {
            response.append("• 5 somun ekmek\n• 10 somun ekmek\n• 1 koli ekmek");
        } else if (product.contains("su")) {
            response.append("• 1 koli su (6'lı)\n• 2 koli su\n• 24'lü su paketi");
        } else {
            response.append("• 5 adet ").append(product).append("\n");
            response.append("• 10 adet ").append(product).append("\n");
            response.append("• 1 koli ").append(product);
        }
        
        return response.toString();
    }
    
    private String generateGeneralQuantityOptions(String product) {
        return String.format("'%s' için kaç adet/paket/kilo istiyorsunuz? Lütfen miktarı belirtir misiniz?", product);
    }

    public String handleEmotionalSupport(String phone, String emotionalQuery, String context) {
        log.info("handleEmotionalSupport() called with query: {}", emotionalQuery);
        
        String emotionType = analyzeEmotionalState(emotionalQuery);
        return generateEmpatheticResponse(emotionType, emotionalQuery, context);
    }
    
    public String handleConfidentPurchase(String phone, String confidenceQuery, String context) {
        log.info("handleConfidentPurchase() called with query: {}", confidenceQuery);
        
        return generateConfidentPurchaseResponse(confidenceQuery, context);
    }
    
    public String handleDelegateRequest(String phone, String delegateQuery, String context) {
        log.info("handleDelegateRequest() called with query: {}", delegateQuery);
        
        return generateDelegateResponse(delegateQuery, context);
    }
    
    public String handleContextualPurchase(String phone, String contextQuery, String context) {
        log.info("handleContextualPurchase() called with query: {}", contextQuery);
        
        String purchaseContext = analyzePurchaseContext(contextQuery);
        return generateContextualSuggestions(purchaseContext, contextQuery);
    }
    
    private String analyzeEmotionalState(String query) {
        String normalized = query.toLowerCase().trim();
        
        if (normalized.contains("üzgün") || normalized.contains("üzül")) return "sad";
        if (normalized.contains("bıktım") || normalized.contains("sıkıl") || normalized.contains("yorgun")) return "frustrated";
        if (normalized.contains("kızgın") || normalized.contains("sinir")) return "angry";
        if (normalized.contains("endişe") || normalized.contains("kaygı")) return "worried";
        if (normalized.contains("mutlu") || normalized.contains("memnun")) return "happy";
        
        return "neutral";
    }
    
    private String generateEmpatheticResponse(String emotionType, String query, String context) {
        StringBuilder response = new StringBuilder();
        
        switch (emotionType) {
            case "sad":
                response.append("😔 Anlıyorum, üzgün hissediyorsunuz. ");
                response.append("Size yardımcı olmak için buradayım. ");
                response.append("Belki küçük bir şeyler sipariş etmek iyi gelir? ");
                response.append("Ne isterseniz, rahat hissettiğiniz şekilde söyleyebilirsiniz.");
                break;
                
            case "frustrated":
                response.append("😤 Anlıyorum, sistem sizi yormuş. ");
                response.append("Daha kolay hale getirebilirim. ");
                response.append("Ne yapmak istediğinizi basitçe söylerseniz, ");
                response.append("ben hallederim. Stres yapmayın! 😊");
                break;
                
            case "angry":
                response.append("😔 Özür dilerim, sizi kızdırdığımız için üzgünüm. ");
                response.append("Sorunu çözmek için elimden geleni yapacağım. ");
                response.append("Nasıl yardımcı olabilirim?");
                break;
                
            case "worried":
                response.append("😌 Endişelenmeyin, her şey yolunda. ");
                response.append("Size adım adım yardım edebilirim. ");
                response.append("Hangi konuda endişeniz var?");
                break;
                
            case "happy":
                response.append("😊 Ne güzel! Mutlu müşteriler bizi de mutlu ediyor. ");
                response.append("Size nasıl yardımcı olabilirim?");
                break;
                
            default:
                response.append("Anlıyorum. Size nasıl yardımcı olabilirim? ");
                response.append("Rahat hissettiğiniz şekilde ihtiyacınızı belirtebilirsiniz.");
                break;
        }
        
        return response.toString();
    }
    
    private String generateConfidentPurchaseResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        response.append("💪 Harika! Kararlı müşteriler seviyoruz. ");
        response.append("Ne almak istediğinizi söylerseniz hemen halledelim. ");
        
        // Context-aware suggestions
        if (context != null && !context.trim().isEmpty()) {
            response.append("Sepetinizde ürünler var, onları mı kastediyorsunuz? ");
            response.append("Yoksa yeni ürünler mi eklemek istiyorsunuz?");
        } else {
            response.append("Hangi ürünleri istiyorsunuz? Listeleyelim!");
        }
        
        return response.toString();
    }
    
    private String generateDelegateResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        
        if (query.toLowerCase().contains("annem") || query.toLowerCase().contains("anne")) {
            response.append("👩 Annenizin isteği için mi sipariş veriyorsunuz? ");
        } else if (query.toLowerCase().contains("eşim") || query.toLowerCase().contains("karım") || query.toLowerCase().contains("kocam")) {
            response.append("💑 Eşinizin isteği için mi sipariş veriyorsunuz? ");
        } else if (query.toLowerCase().contains("çocuk")) {
            response.append("👶 Çocuklarınız için mi sipariş veriyorsunuz? ");
        } else {
            response.append("👥 Başka biri için mi sipariş veriyorsunuz? ");
        }
        
        response.append("Ne istediğini söylerseniz ben hallederim. ");
        response.append("Hangi ürünleri istiyorlar?");
        
        return response.toString();
    }
    
    private String analyzePurchaseContext(String query) {
        String normalized = query.toLowerCase().trim();
        
        if (normalized.contains("çocuk") || normalized.contains("kid")) return "children";
        if (normalized.contains("misafir") || normalized.contains("guest")) return "guests";
        if (normalized.contains("parti") || normalized.contains("party")) return "party";
        if (normalized.contains("kahvaltı") || normalized.contains("breakfast")) return "breakfast";
        if (normalized.contains("akşam") || normalized.contains("dinner")) return "dinner";
        if (normalized.contains("piknik") || normalized.contains("gezi")) return "outing";
        
        return "general";
    }
    
    private String generateContextualSuggestions(String purchaseContext, String query) {
        StringBuilder response = new StringBuilder();
        
        switch (purchaseContext) {
            case "children":
                response.append("👶 **Çocuklar İçin Öneriler:**\n\n");
                response.append("• Süt ve süt ürünleri\n");
                response.append("• Çikolata ve atıştırmalıklar\n");
                response.append("• Meyve suları\n");
                response.append("• Bisküvi ve kekler\n\n");
                response.append("Çocuklarınız için hangi ürünleri istiyorsunuz?");
                break;
                
            case "guests":
                response.append("🏠 **Misafir İçin Öneriler:**\n\n");
                response.append("• Çay, kahve\n");
                response.append("• Çikolata, şeker\n");
                response.append("• Kek, bisküvi\n");
                response.append("• İçecekler\n\n");
                response.append("Kaç kişi gelecek? Ona göre miktarları ayarlayabilirim.");
                break;
                
            case "party":
                response.append("🎉 **Parti İçin Öneriler:**\n\n");
                response.append("• Gazlı içecekler (Kola, Fanta, Sprite)\n");
                response.append("• Cips ve atıştırmalıklar\n");
                response.append("• Su (çok gerekli!)\n");
                response.append("• Çikolata ve şekerlemeler\n\n");
                response.append("Kaç kişilik parti? Buna göre miktarları hesaplayalım!");
                break;
                
            case "breakfast":
                response.append("🍞 **Kahvaltı İçin Öneriler:**\n\n");
                response.append("• Ekmek, simit\n");
                response.append("• Peynir, tereyağı\n");
                response.append("• Süt, yoğurt\n");
                response.append("• Bal, reçel\n\n");
                response.append("Hangi kahvaltı ürünlerini istiyorsunuz?");
                break;
                
            default:
                response.append("🛒 **Özel Durum İçin Yardım:**\n\n");
                response.append("Hangi amaç için alışveriş yapıyorsunuz? ");
                response.append("Size özel önerilerde bulunabilirim!");
                break;
        }
        
        return response.toString();
    }

    public String handleStockIssue(String phone, String stockQuery, String context) {
        log.info("handleStockIssue() called with query: {}", stockQuery);
        
        String affectedProduct = extractAffectedProduct(stockQuery, context);
        return generateStockIssueResponse(affectedProduct, stockQuery);
    }
    
    public String handlePriceChange(String phone, String priceQuery, String context) {
        log.info("handlePriceChange() called with query: {}", priceQuery);
        
        String affectedProduct = extractAffectedProduct(priceQuery, context);
        return generatePriceChangeResponse(affectedProduct, priceQuery);
    }
    
    public String handleSystemRecovery(String phone, String systemQuery, String context) {
        log.info("handleSystemRecovery() called with query: {}", systemQuery);
        
        String issueType = analyzeSystemIssue(systemQuery);
        return generateSystemRecoveryResponse(issueType, systemQuery);
    }
    
    public String handleConflict(String phone, String conflictQuery, String context) {
        log.info("handleConflict() called with query: {}", conflictQuery);
        
        return generateConflictResolutionResponse(conflictQuery, context);
    }
    
    private String extractAffectedProduct(String query, String context) {
        String combined = (query + " " + (context != null ? context : "")).toLowerCase();
        
        // AI-driven product extraction for system issues
        if (combined.contains("coca-cola") || combined.contains("kola")) return "Coca-Cola";
        if (combined.contains("pepsi")) return "Pepsi";
        if (combined.contains("fanta")) return "Fanta";
        if (combined.contains("su")) return "Su";
        if (combined.contains("süt")) return "Süt";
        if (combined.contains("ekmek")) return "Ekmek";
        if (combined.contains("çikolata")) return "Çikolata";
        
        return null; // No specific product identified
    }
    
    private String generateStockIssueResponse(String product, String query) {
        StringBuilder response = new StringBuilder();
        
        if (product != null) {
            response.append("📦 **Stok Durumu:**\n\n");
            response.append("Maalesef ").append(product).append(" şu anda stokta kalmamış. ");
            response.append("Ama size alternatif önerebilirim!\n\n");
            
            // AI-driven alternative suggestions
            response.append("🔄 **Alternatif Seçenekler:**\n");
            if (product.toLowerCase().contains("kola")) {
                response.append("• Pepsi, Fanta, Sprite gibi diğer gazlı içecekler\n");
                response.append("• Farklı marka kolalar\n");
            } else if (product.toLowerCase().contains("su")) {
                response.append("• Farklı marka sular\n");
                response.append("• Farklı boyutlarda su seçenekleri\n");
            } else {
                response.append("• Benzer ürünler mevcut\n");
                response.append("• Farklı marka seçenekleri\n");
            }
            response.append("\nAlternatif ürünleri görmek ister misiniz?");
        } else {
            response.append("📦 **Stok Konusunda Yardım:**\n\n");
            response.append("Hangi ürün için stok sorunu yaşıyorsunuz? ");
            response.append("Size alternatif önerebilirim veya stok durumunu kontrol edebilirim.");
        }
        
        return response.toString();
    }
    
    private String generatePriceChangeResponse(String product, String query) {
        StringBuilder response = new StringBuilder();
        
        response.append("💰 **Fiyat Güncellemesi:**\n\n");
        
        if (product != null) {
            response.append("Evet, ").append(product).append(" fiyatında değişiklik olmuş. ");
            response.append("Güncel fiyatları görmek ister misiniz?\n\n");
            response.append("🔄 **Seçenekleriniz:**\n");
            response.append("• Güncel fiyatlarla devam edin\n");
            response.append("• Daha uygun fiyatlı alternatifler görelim\n");
            response.append("• Sepeti gözden geçirip güncelleyelim\n\n");
            response.append("Nasıl ilerlemek istersiniz?");
        } else {
            response.append("Fiyat konusunda endişeniz var mı? ");
            response.append("Güncel fiyatları gösterebilirim veya ");
            response.append("bütçenize uygun alternatifler önerebilirim.");
        }
        
        return response.toString();
    }
    
    private String analyzeSystemIssue(String query) {
        String normalized = query.toLowerCase().trim();
        
        if (normalized.contains("çalışmıyor") || normalized.contains("bozuk")) return "not_working";
        if (normalized.contains("yavaş") || normalized.contains("geç")) return "slow";
        if (normalized.contains("hata") || normalized.contains("error")) return "error";
        if (normalized.contains("dondu") || normalized.contains("takıldı")) return "frozen";
        
        return "general_issue";
    }
    
    private String generateSystemRecoveryResponse(String issueType, String query) {
        StringBuilder response = new StringBuilder();
        
        switch (issueType) {
            case "not_working":
                response.append("🔧 **Sistem Desteği:**\n\n");
                response.append("Sistem çalışıyor, ben buradayım! ");
                response.append("Belki farklı bir şekilde deneyelim? ");
                response.append("Ne yapmak istediğinizi basitçe söylerseniz, ");
                response.append("size yardımcı olabilirim.");
                break;
                
            case "slow":
                response.append("⏰ **Performans Desteği:**\n\n");
                response.append("Anlıyorum, sistem yavaş gelebiliyor. ");
                response.append("Ben hızlı şekilde yardımcı olabilirim. ");
                response.append("Ne istediğinizi söylerseniz hemen halledelim!");
                break;
                
            case "error":
                response.append("🚨 **Hata Desteği:**\n\n");
                response.append("Bir hata mı yaşadınız? Üzgünüm! ");
                response.append("Yeniden deneyebiliriz. ");
                response.append("Ne yapmaya çalışıyordunuz? Size yardım edeyim.");
                break;
                
            case "frozen":
                response.append("🔄 **Sistem Yenileme:**\n\n");
                response.append("Sistem donmuş gibi mi gözüküyor? ");
                response.append("Ben aktifim ve size yardım edebilirim. ");
                response.append("Ne yapmak istediğinizi tekrar söyler misiniz?");
                break;
                
            default:
                response.append("🛠️ **Teknik Destek:**\n\n");
                response.append("Bir sorun mu yaşıyorsunuz? ");
                response.append("Size yardımcı olmaya çalışayım. ");
                response.append("Sorunu daha detaylı anlatabilir misiniz?");
                break;
        }
        
        return response.toString();
    }
    
    private String generateConflictResolutionResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        
        response.append("🔄 **Çakışma Çözümü:**\n\n");
        response.append("Birden fazla işlem aynı anda mı yapılmaya çalışıldı? ");
        response.append("Sorun değil, ben hallederim!\n\n");
        
        // Context-aware conflict resolution
        if (context != null && !context.trim().isEmpty()) {
            response.append("Mevcut sepet durumunuzu kontrol ettim. ");
            response.append("Ne yapmak istediğinizi tekrar söyler misiniz? ");
            response.append("Size en güncel durumu gösterebilirim.");
        } else {
            response.append("Temiz bir başlangıç yapalım. ");
            response.append("Ne yapmak istediğinizi söylerseniz, ");
            response.append("adım adım halledelim.");
        }
        
        return response.toString();
    }


    public String showProducts() {
        log.info("showProducts() called - fetching all active products");
        List<com.example.marketsupplier.entity.Product> products = productService.getAllActiveProducts();
        log.info("Found {} products", products.size());
        
        if (products.isEmpty()) {
            log.warn("No active products found!");
            return "Şu anda aktif ürün bulunmuyor.";
        }
        
        StringBuilder sb = new StringBuilder("📋 **Mevcut Ürünler:**\n\n");
        
        // Show all products (removed pagination limit)
        for (com.example.marketsupplier.entity.Product p : products) {
            
            // Professional product display with stock info
            sb.append("🔹 **").append(p.getName()).append("**\n");
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                sb.append("   ").append(p.getDescription()).append("\n");
            }
            sb.append("   💰 Fiyat: ").append(p.getPrice()).append(" TL/").append(p.getUnit()).append("\n");
            
            // Stock status with emojis
            Integer stock = p.getStockQuantity();
            if (stock == null || stock <= 0) {
                sb.append("   ❌ Stokta yok\n");
            } else if (stock <= 5) {
                sb.append("   ⚠️ Az stok: ").append(stock).append(" ").append(p.getUnit()).append(" kaldı\n");
            } else if (stock <= 20) {
                sb.append("   ✅ Stokta: ").append(stock).append(" ").append(p.getUnit()).append("\n");
            } else {
                sb.append("   ✅ Bol stokta: ").append(stock).append(" ").append(p.getUnit()).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("\n💡 **Sipariş için örnek:** '3 ").append(products.get(0).getUnit()).append(" ").append(products.get(0).getName()).append("'");
        
        String result = sb.toString();
        log.info("showProducts() returning {} characters", result.length());
        return result;
    }

    public String showProductsByCategory(String categoryQuery) {
        log.info("showProductsByCategory() called with query: {}", categoryQuery);
        List<com.example.marketsupplier.entity.Product> allProducts = productService.getAllActiveProducts();
        
        if (allProducts.isEmpty()) {
            return "Şu anda aktif ürün bulunmuyor.";
        }
        
        // Kategori eşleştirme - AI tabanlı fuzzy matching
        List<com.example.marketsupplier.entity.Product> filteredProducts = filterProductsByCategory(allProducts, categoryQuery);
        
        if (filteredProducts.isEmpty()) {
            return "Bu kategoride ürün bulunamadı. Tüm ürünleri görmek ister misiniz?";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🥤 **").append(getCategoryDisplayName(categoryQuery)).append(" Ürünlerimiz:**\n\n");
        
        for (com.example.marketsupplier.entity.Product product : filteredProducts) {
            sb.append("• **").append(product.getName()).append("**")
              .append(" - ").append(product.getPrice()).append(" TL/").append(product.getUnit());
            
            // Stok durumu kısaca
            Integer stock = product.getStockQuantity();
            if (stock == null || stock <= 0) {
                sb.append(" ❌ Tükendi");
            } else if (stock <= 5) {
                sb.append(" ⚠️ Az kaldı");
            } else {
                sb.append(" ✅ Mevcut");
            }
            sb.append("\n");
        }
        
        sb.append("\n💡 **Sepete eklemek için:** '2 koli Coca-Cola' şeklinde yazabilirsiniz.");
        sb.append("\n🔍 **Hangi ürünü istiyorsunuz?** Tam ismini yazmanız yeterli.");
        
        return sb.toString();
    }
    
    private List<com.example.marketsupplier.entity.Product> filterProductsByCategory(List<com.example.marketsupplier.entity.Product> products, String categoryQuery) {
        String normalizedQuery = categoryQuery.toLowerCase().trim();
        List<com.example.marketsupplier.entity.Product> filtered = new ArrayList<>();
        
        for (com.example.marketsupplier.entity.Product product : products) {
            String productName = product.getName().toLowerCase();
            
            // Gazlı içecekler kategorisi
            if (isDrinkCategory(normalizedQuery)) {
                if (isDrinkProduct(productName)) {
                    filtered.add(product);
                }
            }
            // Su kategorisi
            else if (isWaterCategory(normalizedQuery)) {
                if (isWaterProduct(productName)) {
                    filtered.add(product);
                }
            }
            // Çikolata kategorisi
            else if (isChocolateCategory(normalizedQuery)) {
                if (isChocolateProduct(productName)) {
                    filtered.add(product);
                }
            }
            // Süt ürünleri kategorisi
            else if (isDairyCategory(normalizedQuery)) {
                if (isDairyProduct(productName)) {
                    filtered.add(product);
                }
            }
            // Marka bazlı filtreleme
            else if (isBrandQuery(normalizedQuery)) {
                if (isBrandProduct(productName, normalizedQuery)) {
                    filtered.add(product);
                }
            }
        }
        
        return filtered;
    }
    
    private boolean isDrinkCategory(String query) {
        return query.contains("kola") || query.contains("cola") || query.contains("gazoz") || 
               query.contains("kazoz") || query.contains("sarı kola") || query.contains("içecek") ||
               query.contains("gazlı") || query.contains("soda");
    }
    
    private boolean isDrinkProduct(String productName) {
        return productName.contains("coca-cola") || productName.contains("pepsi") || 
               productName.contains("fanta") || productName.contains("yedigün") || 
               productName.contains("gazoz") || productName.contains("kola") ||
               productName.contains("sprite") || productName.contains("schweppes") ||
               productName.contains("cola") || productName.contains("soda");
    }
    
    private boolean isWaterCategory(String query) {
        return query.contains("su") || query.contains("water");
    }
    
    private boolean isWaterProduct(String productName) {
        return productName.contains("su") && !productName.contains("susam");
    }
    
    private boolean isChocolateCategory(String query) {
        return query.contains("çikolata") || query.contains("chocolate");
    }
    
    private boolean isChocolateProduct(String productName) {
        return productName.contains("çikolata") || productName.contains("chocolate") ||
               productName.contains("albeni") || productName.contains("ülker");
    }
    
    private boolean isDairyCategory(String query) {
        return query.contains("süt") || query.contains("milk") || query.contains("yoğurt") ||
               query.contains("peynir") || query.contains("cheese");
    }
    
    private boolean isDairyProduct(String productName) {
        return productName.contains("süt") || productName.contains("yoğurt") || 
               productName.contains("peynir") || productName.contains("milk");
    }
    
    private boolean isBrandQuery(String query) {
        return query.contains("ülker") || query.contains("ulker") ||
               query.contains("nestle") || query.contains("nestlé") ||
               query.contains("coca-cola") || query.contains("pepsi") ||
               query.contains("unilever") || query.contains("pınar") ||
               query.contains("eti") || query.contains("torku");
    }
    
    private boolean isBrandProduct(String productName, String brandQuery) {
        String brand = extractBrandName(brandQuery);
        return productName.contains(brand);
    }
    
    private String extractBrandName(String query) {
        String normalized = query.toLowerCase().trim();
        if (normalized.contains("ülker") || normalized.contains("ulker")) return "ülker";
        if (normalized.contains("nestle") || normalized.contains("nestlé")) return "nestle";
        if (normalized.contains("coca-cola")) return "coca-cola";
        if (normalized.contains("pepsi")) return "pepsi";
        if (normalized.contains("unilever")) return "unilever";
        if (normalized.contains("pınar")) return "pınar";
        if (normalized.contains("eti")) return "eti";
        if (normalized.contains("torku")) return "torku";
        return normalized; // fallback
    }
    
    private String getCategoryDisplayName(String categoryQuery) {
        String normalized = categoryQuery.toLowerCase().trim();
        if (isDrinkCategory(normalized)) return "Gazlı İçecek";
        if (isWaterCategory(normalized)) return "Su";
        if (isChocolateCategory(normalized)) return "Çikolata";
        if (isDairyCategory(normalized)) return "Süt Ürünleri";
        if (isBrandQuery(normalized)) return extractBrandName(normalized).toUpperCase() + " Ürünleri";
        return "Ürün";
    }

    // Sayfalı ürün gösterimi (WhatsApp için "başka var mı?" akışı)
    public String showProductsPage(com.example.marketsupplier.service.CustomerContext ctx, int page) {
        List<com.example.marketsupplier.entity.Product> products = productService.getAllActiveProducts();
        if (products.isEmpty()) return "Şu anda aktif ürün bulunmuyor.";
        int pageSize = 10;
        int from = Math.max(0, page * pageSize);
        if (from >= products.size()) {
            return "Liste sonuna ulaştık. Farklı ürün ister misiniz?";
        }
        int to = Math.min(products.size(), from + pageSize);
        StringBuilder sb = new StringBuilder("Mevcut ürünler (" + (page + 1) + ". sayfa):\n");
        for (int i = from; i < to; i++) {
            com.example.marketsupplier.entity.Product p = products.get(i);
            sb.append("- ").append(p.getName()).append(" (")
              .append(p.getUnit()).append(") - ")
              .append(p.getPrice()).append(" TL\n");
        }
        ctx.getSessionData().put("products_page", page);
        if (to < products.size()) {
            sb.append("Devam etmek için 'başka var mı' yazabilirsiniz.");
        }
        return sb.toString();
    }

    public String showNextProducts(com.example.marketsupplier.service.CustomerContext ctx) {
        Object p = ctx.getSessionData().get("products_page");
        int next = (p instanceof Integer) ? ((Integer) p) + 1 : 0;
        return showProductsPage(ctx, next);
    }

    public String reserveItems(String phone, List<Item> items) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Sisteme kayıtlı bir marketiniz bulunamadı.";
        }

        List<Product> allProducts = productService.getAllActiveProducts();
        List<CartItem> currentCartItems = cartService.getItems(market.getId());
        List<String> addedItems = new ArrayList<>();
        List<String> partiallyAdded = new ArrayList<>();
        List<String> outOfStockItems = new ArrayList<>();
        List<String> notFoundItems = new ArrayList<>();
        List<String> duplicateItems = new ArrayList<>();
        List<String> forbiddenItems = new ArrayList<>();
        List<String> correctedUnits = new ArrayList<>();
        List<String> excessiveAmounts = new ArrayList<>();
        List<String> alternativeBrandNotes = new ArrayList<>();
        List<String> clarifyingQuestions = new ArrayList<>();

        // AI'nın anlayacağı şekilde duplicate detection - manuel if'ler yok
        Map<String, List<Item>> productGroups = new HashMap<>();
        for (Item item : items) {
            if (item == null) continue;
            // Aşırı miktar filtreleme (insan hatası): 1000 ve üzeri miktarlarda uyar
            if (item.getQty() > 1000) {
                excessiveAmounts.add(String.format("%s için istenen miktar (%d) gerçekçi değil. Lütfen kontrol edin.",
                        item.getProductQuery(), item.getQty()));
                continue;
            }

            // Yasaklı/yaşa bağlı ürünler (sistem satmıyor olabilir)
            if (isForbiddenRequest(item.getProductQuery())) {
                forbiddenItems.add(item.getProductQuery());
                continue;
            }

            // Kategori/görece genel istekleri netleştir
            if (isCategoryQuery(item.getProductQuery())) {
                clarifyingQuestions.add(String.format("'%s' oldukça genel. Hangi ürün(ler)i ve miktarı belirtir misiniz? Örn: '2 koli Cola'", item.getProductQuery()));
                continue;
            }

            Product bestMatch = findBestMatch(item.getProductQuery(), allProducts);
            if (bestMatch != null) {
                String productKey = bestMatch.getId().toString();
                productGroups.computeIfAbsent(productKey, k -> new ArrayList<>()).add(item);
            }
        }

        // Her ürün grubu için toplam miktarı hesapla
        for (Map.Entry<String, List<Item>> entry : productGroups.entrySet()) {
            String productKey = entry.getKey();
            List<Item> productItems = entry.getValue();
            
            if (productItems.size() > 1) {
                // Aynı üründen birden fazla miktar istenmiş
                int totalQty = productItems.stream().mapToInt(Item::getQty).sum();
                String productName = productItems.get(0).getProductQuery();
                duplicateItems.add(String.format("%s için %d farklı miktar istediniz, toplam %d olarak birleştirdim", 
                    productName, productItems.size(), totalQty));
                
                // İlk item'ı toplam miktarla güncelle
                Item firstItem = productItems.get(0);
                firstItem.setQty(totalQty);
            }
        }

        for (Item item : items) {
            Product bestMatch = findBestMatch(item.getProductQuery(), allProducts);
            log.info("DEBUG: Looking for '{}' -> Found: {} (ID: {}, Price: {} TL, Unit: {})", 
                item.getProductQuery(), 
                bestMatch != null ? bestMatch.getName() : "NOT_FOUND",
                bestMatch != null ? bestMatch.getId() : "N/A",
                bestMatch != null ? bestMatch.getPrice() : "N/A",
                bestMatch != null ? bestMatch.getUnit() : "N/A");

            if (bestMatch != null) {
                int requestedQty = item.getQty();
                if (requestedQty <= 0) {
                    continue;
                }

                // Birim uyumsuzluğu düzeltmeleri (ör: süt litre, ekmek/adet, yumurta/koli vb.)
                String decidedUnit = decideUnit(bestMatch, item.getUnit());
                if (item.getUnit() != null && !item.getUnit().equalsIgnoreCase(decidedUnit)) {
                    correctedUnits.add(String.format("%s için birim '%s' yerine '%s' olarak güncellendi.",
                            bestMatch.getName(), item.getUnit(), decidedUnit));
                }

                Integer stockQuantityObj = bestMatch.getStockQuantity();
                int stockQuantity = stockQuantityObj != null ? stockQuantityObj : Integer.MAX_VALUE; // stok girilmemişse limitsiz kabul et

                // Sepette bu üründen zaten var ise, kalan stok hesapla
                int existingInCart = currentCartItems.stream()
                        .filter(ci -> Objects.equals(ci.getProductId(), bestMatch.getId()))
                        .mapToInt(CartItem::getQuantity)
                        .sum();
                int available = Math.max(0, stockQuantity - existingInCart);

                if (available == 0) {
                    outOfStockItems.add(String.format("%s (stok yok)", bestMatch.getName()));
                    continue;
                }

                int toAdd = Math.min(requestedQty, available);
                log.info("DEBUG: Adding to cart - Product: {}, ID: {}, Price: {} TL, Quantity: {}, Total: {} TL", 
                    bestMatch.getName(), bestMatch.getId(), bestMatch.getPrice(), toAdd, 
                    bestMatch.getPrice().multiply(BigDecimal.valueOf(toAdd)));
                cartService.addItemToCart(market, bestMatch, toAdd, decidedUnit);
                if (toAdd == requestedQty) {
                    addedItems.add(String.format("%d %s %s", toAdd, decidedUnit != null ? decidedUnit : bestMatch.getUnit(), bestMatch.getName()));
                } else {
                    partiallyAdded.add(String.format("%s için %d istediniz, stokta %d var. %d eklendi.",
                            bestMatch.getName(), requestedQty, available, toAdd));
                }
            } else {
                notFoundItems.add(item.getProductQuery());
                // Marka uyuşmazlığı olabilir, benzer ürün önerisi notu bırak
                Product alt = suggestAlternative(item.getProductQuery(), allProducts);
                if (alt != null) {
                    alternativeBrandNotes.add(String.format("'%s' bulunamadı; benzer olarak '%s' mevcut.",
                            item.getProductQuery(), alt.getName()));
                }
            }
        }
        
        StringBuilder response = new StringBuilder();
        if (!duplicateItems.isEmpty()) {
            response.append("ℹ️ ").append(String.join("\n", duplicateItems)).append("\n\n");
        }
        if (!clarifyingQuestions.isEmpty()) {
            response.append("❓ Netleştirme gerekli:\n").append(String.join("\n", clarifyingQuestions)).append("\n\n");
        }
        if (!correctedUnits.isEmpty()) {
            response.append("🔁 Birim düzeltmeleri:\n").append(String.join("\n", correctedUnits)).append("\n\n");
        }
        if (!addedItems.isEmpty()) {
            response.append("✅ Sepetinize eklendi: ").append(String.join(", ", addedItems)).append(". ");
        }
        if (!partiallyAdded.isEmpty()) {
            response.append("\n⚠️ Kısmi eklenenler: \n").append(String.join("\n", partiallyAdded)).append("\n");
        }
        if (!outOfStockItems.isEmpty()) {
            response.append("\n❌ Stokta olmayanlar: ").append(String.join(", ", outOfStockItems)).append(". ");
        }
        if (!notFoundItems.isEmpty()) {
            response.append("\n🔍 Bulunamayan ürünler: ").append(String.join(", ", notFoundItems)).append(". ");
        }
        if (!alternativeBrandNotes.isEmpty()) {
            response.append("\n🤝 Alternatif marka/ürün: ").append(String.join("; ", alternativeBrandNotes)).append(". ");
        }
        if (!forbiddenItems.isEmpty()) {
            response.append("\n🚫 Satışta olmayan/yasaklı: ").append(String.join(", ", forbiddenItems)).append(". ");
        }
        if (!excessiveAmounts.isEmpty()) {
            response.append("\n🧮 Miktar uyarıları: ").append(String.join("; ", excessiveAmounts)).append(". ");
        }
        
        // Her eklemeden sonra güncel sepeti göster
        response.append("\n\n").append(showCart(phone));

        return response.toString();
    }

    public String showCart(String phone) {
        Market market = findMarketByPhone(phone);
        if (market == null) return "Numaranız sisteme kayıtlı değil.";
        List<com.example.marketsupplier.entity.CartItem> items = cartService.getItems(market.getId());
        if (items.isEmpty()) return "Sepetiniz boş.";
        StringBuilder sb = new StringBuilder("Sepetiniz:\n");
        BigDecimal total = BigDecimal.ZERO;
        for (com.example.marketsupplier.entity.CartItem item : items) {
            sb.append("- ").append(item.getProductName()).append(" x").append(item.getQuantity())
                    .append(" ").append(item.getUnit()).append(" - ").append(item.getPrice()).append(" TL\n");
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            log.info("DEBUG: Cart item - Product: {}, ID: {}, UnitPrice: {}, Qty: {}, ItemTotal: {}", 
                item.getProductName(), item.getProductId(), item.getPrice(), item.getQuantity(), itemTotal);
            total = total.add(itemTotal);
        }
        sb.append(String.format(Locale.forLanguageTag("tr-TR"), "Toplam: %.2f TL", total));
        sb.append("\nOnaylamak için 'onayla' yazabilirsiniz.");
        return sb.toString();
    }

    public String finalizeOrder(String phone) {
        Long marketId = marketService.findByPhoneNormalized(phone).map(m -> m.getId()).orElse(null);
        if (marketId == null) return "Numaranız sisteme kayıtlı değil.";
        com.example.marketsupplier.entity.Order order = orderService.createOrder(marketId);
        List<com.example.marketsupplier.entity.CartItem> items = cartService.getItems(marketId);
        for (com.example.marketsupplier.entity.CartItem ci : items) {
            orderService.addItemToOrder(order.getId(), ci.getProductName(), ci.getQuantity(), ci.getUnit(), ci.getPrice());
        }
        if (audit != null) audit.record(String.valueOf(marketId), String.valueOf(marketId), "finalize_order", "{orderId:" + order.getId() + "}");
        return "Siparişiniz oluşturuldu. Numara: #" + order.getId();
    }

    public String confirmOrder(String phone) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Numaranız sisteme kayıtlı değil.";
        }

        List<CartItem> cartItems = cartService.getItems(market.getId());
        if (cartItems.isEmpty()) {
            // Akıllı yönlendirme:  crfcn katalo funu g f6ster ve soruyla y f6nlendir
            StringBuilder sb = new StringBuilder();
            sb.append("Sepetiniz boş görünüyor. Sipariş vermek için önce ürün ekleyelim.\n\n");
            sb.append(showProducts());
            sb.append("\n\nHangi ürünleri ekleyelim? (örn: '5 koli Coca-Cola, 3 koli Pepsi')");
            return sb.toString();
        }

        com.example.marketsupplier.entity.Order order = orderService.createOrderFromCart(market.getId());
        if (order == null) {
            return "Sipariş oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.";
        }

        // Sipariş sonrası sepeti temizle
        cartService.clearCart(market.getId());

        if (audit != null) {
            audit.record(String.valueOf(market.getId()), String.valueOf(market.getId()), "confirm_order", "{orderId:" + order.getId() + "}");
        }

        StringBuilder confirmationMessage = new StringBuilder();
        confirmationMessage.append("🎉 Siparişiniz başarıyla alındı!\n");
        confirmationMessage.append("Sipariş Numaranız: #").append(order.getId()).append("\n\n");
        confirmationMessage.append("Sipariş Detayları:\n");
        order.getItems().forEach(item -> {
            confirmationMessage.append(String.format("- %s x%d %s\n", item.getProductName(), item.getQuantity(), item.getUnit()));
        });
        confirmationMessage.append("\n").append(String.format(Locale.forLanguageTag("tr-TR"), "Toplam Tutar: %.2f TL", order.getTotalPrice()));
        confirmationMessage.append("\n\nEn kısa sürede hazırlanıp size ulaştırılacaktır. Teşekkür ederiz!");

        return confirmationMessage.toString();
    }

    public String cancelOrder(String phone) {
        return "Sipariş işlemi iptal edildi. Sepetinizdeki ürünlerle alışverişe devam edebilirsiniz.";
    }

    public String removeFromCart(String phone, List<Item> items) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Sisteme kayıtlı bir marketiniz bulunamadı.";
        }
        List<CartItem> cartItems = cartService.getItems(market.getId());
        List<Product> allProducts = productService.getAllActiveProducts();

        List<String> removed = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        // İsim verilmemişse ve tek ürün varsa otomatik sil (state tabanlı)
        if ((items == null || items.isEmpty())) {
            if (cartItems.size() == 1) {
                cartService.removeItem(market.getId(), cartItems.get(0).getId());
                return "✅ " + cartItems.get(0).getProductName() + " sepetten çıkarıldı.";
            }
            // Birden fazla ürün varsa isim olmadan çıkarma yapmayız
            return "Sepetinizde bu ürün bulunamadı.";
        }

        for (Item item : items) {
            if (item.getProductQuery() == null || item.getProductQuery().isBlank()) {
                continue;
            }
            // Ürün adına göre eşleşen sepet öğelerini bul ve çıkar
            Product bestMatch = findBestMatch(item.getProductQuery(), allProducts);

            if (bestMatch != null) {
                Optional<CartItem> itemToRemoveOpt = cartItems.stream()
                    .filter(ci -> Objects.equals(ci.getProductId(), bestMatch.getId()))
                    .findFirst();

                if (itemToRemoveOpt.isPresent()) {
                    cartService.removeItem(market.getId(), itemToRemoveOpt.get().getId());
                    removed.add(bestMatch.getName());
                    continue;
                }
            }

            // Fallback: ürün kataloğunda bulunamadıysa, sepetteki ürün adlarına göre bulanık eşleştir ve isimle sil
            String matchedCartName = findBestCartNameMatch(item.getProductQuery(), cartItems);
            if (matchedCartName != null) {
                cartService.removeItemByName(market.getId(), matchedCartName);
                removed.add(matchedCartName);
            } else {
                notFound.add(item.getProductQuery());
            }
        }

        if (removed.isEmpty() && notFound.isEmpty()) {
            return "Sepetinizde bu ürün bulunamadı.";
        }

        // Şablonlaştırılmış cevaplar
        if (removed.size() == 1 && notFound.isEmpty()) {
            return "✅ " + removed.get(0) + " sepetten çıkarıldı.";
        }

        StringBuilder response = new StringBuilder();
        if (!removed.isEmpty()) {
            response.append("✅ Sepetinizden çıkarıldı: ").append(String.join(", ", removed)).append(". ");
        }
        if (!notFound.isEmpty()) {
            response.append("Bulunamadı: ").append(String.join(", ", notFound)).append(". ");
        }
        return response.toString() + "\n\n" + showCart(phone);
    }

    public String updateCart(String phone, List<Item> items) {
        Market market = findMarketByPhone(phone);
        if (market == null) {
            return "Sisteme kayıtlı bir marketiniz bulunamadı.";
        }
        List<Product> allProducts = productService.getAllActiveProducts();
        List<CartItem> cartItems = cartService.getItems(market.getId());
        
        List<String> updated = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (Item item : items) {
            if (item.getProductQuery() == null || item.getProductQuery().isBlank()) {
                continue;
            }
            Product bestMatch = findBestMatch(item.getProductQuery(), allProducts);
            
            if (bestMatch != null) {
                Optional<CartItem> itemToUpdateOpt = cartItems.stream()
                    .filter(ci -> ci.getProductId().equals(bestMatch.getId()))
                    .findFirst();
                
                if(itemToUpdateOpt.isPresent()){
                    cartService.updateItemQuantity(market.getId(), itemToUpdateOpt.get().getId(), item.getQty());
                    updated.add(item.getQty() + " " + (item.getUnit() != null ? item.getUnit() : bestMatch.getUnit()) + " " + bestMatch.getName());
                } else {
                    // Item not in cart, add it instead? Or report not found. Let's report.
                    notFound.add(item.getProductQuery());
                }
            } else {
                notFound.add(item.getProductQuery());
            }
        }

        if (updated.isEmpty() && notFound.isEmpty()) {
            return "Sepetinizde güncellenecek ürün bulunamadı.";
        }
        
        StringBuilder response = new StringBuilder();
        if (!updated.isEmpty()){
            response.append("Sepetiniz güncellendi: ").append(String.join(", ", updated)).append(". ");
        }
        if (!notFound.isEmpty()){
             response.append("Ancak şu ürünler sepetinizde bulunamadı: ").append(String.join(", ", notFound)).append(". ");
        }

        return response.toString() + "\n\n" + showCart(phone);
    }

    public String clearCart(String phone) {
        log.info("clearCart() called for phone: {}", phone);
        Market market = findMarketByPhone(phone);
        if (market == null) {
            log.warn("Market not found for phone: {}", phone);
            return "Numaranız sisteme kayıtlı değil.";
        }
        
        List<com.example.marketsupplier.entity.CartItem> cartItems = cartService.getItems(market.getId());
        log.info("Cart items before clear: {} for market: {}", cartItems.size(), market.getId());
        
        if (cartItems.isEmpty()) {
            log.info("Cart already empty for market: {}", market.getId());
            return "🛒 Sepetiniz zaten boş.";
        }
        
        cartService.clearCart(market.getId());
        log.info("Cart cleared for market: {}", market.getId());
        
        // Wait for transaction to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify cart is actually cleared
        List<com.example.marketsupplier.entity.CartItem> remainingItems = cartService.getItems(market.getId());
        log.info("Cart items after clear: {} for market: {}", remainingItems.size(), market.getId());
        
        if (!remainingItems.isEmpty()) {
            log.error("CRITICAL: Cart was not cleared! {} items remaining", remainingItems.size());
            return "❌ Sepet boşaltılamadı, tekrar deneyin.";
        }
        
        if (audit != null) audit.record(String.valueOf(market.getId()), String.valueOf(market.getId()), "clear_cart", "{}");
        return "✅ Sepetinizi boşalttım. Başka bir şeyle yardımcı olabilir miyim?";
    }

    private String findBestCartNameMatch(String query, List<CartItem> cartItems) {
        if (query == null || query.isBlank()) return null;
        String qn = stringNormalizer.normalizeForProductMatching(query);
        double best = 0.0;
        String bestName = null;
        for (CartItem ci : cartItems) {
            String cn = stringNormalizer.normalize(ci.getProductName());
            double score = combinedSimilarity(qn, cn);
            if (score > best) { best = score; bestName = ci.getProductName(); }
        }
        return best >= 0.6 ? bestName : null;
    }

    private Product findBestMatch(String query, List<Product> products) {
        if (query == null || query.isBlank()) {
            return null;
        }
        
        final String normalizedQuery = stringNormalizer.normalizeForProductMatching(query);
        log.info("Finding best match for query: '{}' -> normalized: '{}'", query, normalizedQuery);

        double threshold = 0.45; // Daha da düşürüldü - çok esnek eşleşme (cizivis->çiziviç)
        
        // Debug: Top 3 matching'leri logla
        products.stream()
                .map(p -> {
                    String pn = stringNormalizer.normalize(p.getName());
                    double score = combinedSimilarity(normalizedQuery, pn);
                    return Map.entry(p, score);
                })
                .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> log.info("Match candidate: '{}' -> score: {}", e.getKey().getName(), e.getValue()));
        
        return products.stream()
                .map(p -> {
                    String pn = stringNormalizer.normalize(p.getName());
                    double score = combinedSimilarity(normalizedQuery, pn);
                    return Map.entry(p, score);
                })
                .filter(e -> e.getValue() >= threshold)
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // --- Gelişmiş benzerlik hesapları ---
    private double combinedSimilarity(String a, String b) {
        // Jaro-Winkler ve trigram benzerliğinin ağırlıklı ortalaması
        double jw = jaroWinkler(a, b);
        double tri = trigramSimilarity(a, b);
        // Kelime örtüşmesi bonusu
        double word = wordOverlap(a, b);
        return 0.6 * jw + 0.3 * tri + 0.1 * word;
    }

    private double jaroWinkler(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int[] mtp = matches(s1, s2);
        double m = mtp[0];
        if (m == 0) return 0.0;
        double j = (m / s1.length() + m / s2.length() + (m - mtp[1]) / m) / 3.0;
        double jw = j < 0.7 ? j : j + Math.min(0.1, 1.0 / mtp[3]) * mtp[2] * (1 - j);
        return jw;
    }

    private int[] matches(String s1, String s2) {
        String max = s1.length() > s2.length() ? s1 : s2;
        String min = s1.length() > s2.length() ? s2 : s1;
        int range = Math.max(max.length() / 2 - 1, 0);
        boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;
        for (int i = 0; i < min.length(); i++) {
            int start = Math.max(i - range, 0);
            int end = Math.min(i + range + 1, max.length());
            for (int j = start; j < end; j++) {
                if (!matchFlags[j] && min.charAt(i) == max.charAt(j)) {
                    matchFlags[j] = true;
                    matches++;
                    break;
                }
            }
        }
        int t = 0; int k = 0;
        boolean[] matchFlags2 = new boolean[min.length()];
        for (int i = 0; i < min.length(); i++) {
            int start = Math.max(i - range, 0);
            int end = Math.min(i + range + 1, max.length());
            for (int j = start; j < end; j++) {
                if (!matchFlags2[i] && min.charAt(i) == max.charAt(j)) {
                    matchFlags2[i] = true;
                    if (k > j) t++;
                    k = j;
                    break;
                }
            }
        }
        int l = 0;
        int maxPrefix = 4;
        for (; l < Math.min(Math.min(s1.length(), s2.length()), maxPrefix) && s1.charAt(l) == s2.charAt(l); l++);
        return new int[]{matches, t, l, max.length()};
    }

    private double trigramSimilarity(String a, String b) {
        Set<String> ta = trigrams(a);
        Set<String> tb = trigrams(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        int inter = 0;
        for (String t : ta) if (tb.contains(t)) inter++;
        int union = ta.size() + tb.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }

    private Set<String> trigrams(String s) {
        Set<String> set = new HashSet<>();
        String x = s.replaceAll("\\s+", " ").trim();
        if (x.length() < 3) { if (!x.isEmpty()) set.add(x); return set; }
        for (int i = 0; i < x.length() - 2; i++) set.add(x.substring(i, i + 3));
        return set;
    }

    private double wordOverlap(String a, String b) {
        Set<String> wa = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> wb = new HashSet<>(Arrays.asList(b.split("\\s+")));
        if (wa.isEmpty() && wb.isEmpty()) return 1.0;
        int inter = 0;
        for (String w : wa) if (wb.contains(w)) inter++;
        int union = wa.size() + wb.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }

    private boolean isCategoryQuery(String query) {
        if (query == null) return false;
        String q = stringNormalizer.normalize(query);
        // Basit kategori kelimeleri; genişletilebilir (property ile de yönetilebilir)
        String[] categories = new String[]{"icecek","icecek","temizlik","meyve","sebze","atistirmalik","kahvalti","sut","sarkuteri","firin","kagit","kozmetik"};
        for (String c : categories) {
            if (q.contains(c)) return true;
        }
        return false;
    }

    private Market findMarketByPhone(String phone) {
        return marketService.findByPhoneNormalized(phone).orElse(null);
    }

    private boolean isForbiddenRequest(String query) {
        if (query == null) return false;
        String q = stringNormalizer.normalize(query);
        // İş kurallarınıza göre genişletilebilir
        return q.contains("sigara") || q.contains("alkol") || q.contains("uyusturucu") || q.contains("uyuşturucu");
    }

    private String decideUnit(Product product, String requestedUnit) {
        // Basit akıllı seçim: ürünün varsayılan birimi varsa onu tercih et
        if (product.getUnit() != null && !product.getUnit().isBlank()) {
            return product.getUnit();
        }
        return requestedUnit; // fallback
    }

    private Product suggestAlternative(String query, List<Product> products) {
        if (query == null || query.isBlank()) return null;
        String q = stringNormalizer.normalize(query);
        return products.stream()
                .map(p -> Map.entry(p, combinedSimilarity(q, stringNormalizer.normalize(p.getName()))))
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
