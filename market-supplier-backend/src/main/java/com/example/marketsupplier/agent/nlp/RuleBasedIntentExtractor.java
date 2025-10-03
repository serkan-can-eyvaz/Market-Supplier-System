package com.example.marketsupplier.agent.nlp;

import com.example.marketsupplier.agent.model.NluResult;
import com.example.marketsupplier.agent.model.Item;
import com.example.marketsupplier.service.CustomerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIntentExtractor implements IntentExtractor {

    private static final Pattern QTY_PATTERN = Pattern.compile("(\\d+)");
    // quantity-first: 5 koli Pepsi, 3 kg Domates, 2 adet su
    private static final Pattern QTY_FIRST = Pattern.compile("(\\d+)\\s*(koli|kg|kilo|adet)?\\s*([\\p{L}0-9\\-\\s]+)", Pattern.CASE_INSENSITIVE);
    // product-first: Pepsi 5 koli, Domates 3 kg
    private static final Pattern PROD_FIRST = Pattern.compile("([\\p{L}0-9\\-\\s]+?)\\s*(\\d+)\\s*(koli|kg|kilo|adet)?", Pattern.CASE_INSENSITIVE);

    @Override
    public NluResult extract(String text) {
        return extract(text, null);
    }
    
    @Override
    public NluResult extract(String text, CustomerContext context) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String norm = normalizeAscii(lower);
        NluResult result = new NluResult();
        result.setConfidence(0.9);
        
        // Context-aware intent detection
        boolean hasActiveCart = context != null && context.isHasActiveCart();

        // Sipariş onaylama
        if (lower.contains("onayla") || lower.contains("onaylıyorum") || lower.contains("tamam") || 
            lower.contains("evet")) {
            result.setIntent("confirm_order");
            return result;
        }
        
        // Not: view_cart kontrolünü clear/remove sonrası çalıştıracağız
        
        // Sipariş başlatma (ürün listesine yönlendir)
        if ((norm.contains("siparis") && (norm.contains("ver") || norm.contains("verelim") || norm.contains("vermek istiyorum") || norm.contains("olustur")))) {
            result.setIntent("get_products");
            return result;
        }
        
        // Ürün listesi
        if (norm.contains("var mi") || norm.contains("urun") || norm.contains("liste") || 
            norm.contains("gormek") || norm.contains("urunler") || norm.contains("ne var") ||
            norm.contains("hangi urunler") || norm.contains("mevcut urunler")) {
            result.setIntent("get_products");
            return result;
        }
        
        // Sepet temizleme (önce clear'ı tespit et; "her şeyi sil" gibi kalıplar remove'a kaçmasın)
        if (norm.contains("sepeti temizle") || norm.contains("sepeti bosalt") || 
            norm.contains("her seyi sil") || norm.contains("herseyi sil") ||
            norm.contains("her sey sil") || norm.contains("hersey sil") ||
            norm.contains("hepsini sil") || norm.contains("hepsi sil") || norm.matches(".*her\\s*(sey|seyi)\\s*sil.*") ||
            norm.matches(".*hepsini\\s*sil.*") ||
            norm.contains("sepeti sifirla") ||
            (norm.contains("sepet") && (norm.contains("temizle") || norm.contains("bosalt") || norm.matches(".*sepet.*bos.*")))) {
            result.setIntent("clear_cart");
            return result;
        }

        // Sepetten çıkarma işlemleri (clear tespitinden SONRA değerlendir)
        if (norm.contains("cikar") || norm.contains("cikart") || norm.contains("sil") || norm.contains("kaldir") || 
            norm.contains("sepetten cikar") || norm.contains("sepetten sil") || 
            norm.contains("sepetten kaldir") || norm.contains("artik istemiyorum") ||
            norm.contains("almak istemiyorum") || norm.contains("vazgectim")) {
            result.setIntent("remove_from_cart");
            // Ürün adı çıkarımı: komut kelimelerini temizleyip kalan metni ürün sorgusu olarak ekle
            String candidate = norm
                .replace("sepetten cikar"," ")
                .replace("sepetten sil"," ")
                .replace("sepetten kaldir"," ")
                .replace("cikart"," ")
                .replace("cikar"," ")
                .replace("sil"," ")
                .replace("kaldir"," ")
                .replace("artik istemiyorum"," ")
                .replace("almak istemiyorum"," ")
                .replaceAll("\\s+", " ")
                .trim();
            candidate = normalizeProductQuery(candidate);
            if (!candidate.isBlank()) {
                List<Item> items = new ArrayList<>();
                items.add(buildItem(text, candidate, 1, null));
                result.setItems(items);
            }
            return result;
        }

        // Sepet görüntüleme (sadece açık komutlarla)
        if (norm.contains("sepeti goster") || norm.contains("sepetimi goster") || norm.contains("sepeti goruntule")) {
            result.setIntent("view_cart");
            return result;
        }

        // Sepet güncelleme işlemleri
        if (norm.contains("guncelle") || norm.contains("degistir") || norm.contains("duzenle") ||
            norm.contains("miktar") || norm.contains("adet") || norm.contains("kac") ||
            norm.contains("daha fazla") || norm.contains("azalt") || norm.contains("artir") ||
            norm.contains("yap") || norm.contains("yapalim")) {
            result.setIntent("update_cart");
            return result;
        }
        
        // Context-aware: Eğer aktif sepet varsa ve sadece ürün adı söyleniyorsa, sepete ekleme
        if (hasActiveCart && !norm.contains("sepet") && !norm.contains("goster") && 
            !norm.contains("cikar") && !norm.contains("sil") && !norm.contains("guncelle")) {
            // Sadece ürün adı ve miktar içeren mesajlar sepete ekleme olarak yorumlanır
            if (norm.matches(".*\\d+.*") || norm.contains("ekle") || norm.contains("alabilir") || 
                norm.contains("istiyorum") || norm.contains("almak")) {
                // Aşağıdaki default parsing'e devam et
            } else {
                result.setIntent("chat");
                result.setConfidence(0.5);
                return result;
            }
        }

        // default: try add_to_cart parse (multi-item, supports 've', comma, newline)
        List<Item> items = new ArrayList<>();
        for (String chunk : text.split("[,\n]|\\sve\\s")) {
            String part = chunk.trim();
            if (part.isEmpty()) continue;

            boolean matched = false;
            Matcher m1 = QTY_FIRST.matcher(part);
            if (m1.find()) {
                int qty = safeInt(m1.group(1), 1);
                String unit = normalizeUnit(m1.group(2));
                String pq = normalizeProductQuery(m1.group(3));
                if (!pq.isBlank()) {
                    items.add(buildItem(part, pq, qty, unit));
                    matched = true;
                }
            }
            if (!matched) {
                Matcher m2 = PROD_FIRST.matcher(part);
                if (m2.find()) {
                    String pq = normalizeProductQuery(m2.group(1));
                    int qty = safeInt(m2.group(2), 1);
                    String unit = normalizeUnit(m2.group(3));
                    if (!pq.isBlank()) items.add(buildItem(part, pq, qty, unit));
                } else {
                    // Fallback minimal: detect only qty and take rest as product
                    Matcher qm = QTY_PATTERN.matcher(part);
                    int qty = qm.find() ? safeInt(qm.group(1), 1) : 1;
                    String pq = normalizeProductQuery(part);
                    if (!pq.isBlank()) items.add(buildItem(part, pq, qty, null));
                }
            }
        }

        if (items.isEmpty()) {
            result.setIntent("chat");
            result.setConfidence(0.5);
        } else {
            result.setIntent("add_to_cart");
            result.setItems(items);
        }
        return result;
    }

    private String normalizeAscii(String s) {
        if (s == null) return "";
        // Türkçe karakterleri ASCII karşılıklarına indirgeme ve yaygın bozulmaları normalize et
        return s
            .replace('ş','s').replace('Ş','S')
            .replace('ı','i').replace('İ','I')
            .replace('ç','c').replace('Ç','C')
            .replace('ğ','g').replace('Ğ','G')
            .replace('ö','o').replace('Ö','O')
            .replace('ü','u').replace('Ü','U')
            .replace("öğ","og").replace("ğü","gu")
            .replace("şğ","sg");
    }

    private Item buildItem(String raw, String productQuery, int qty, String unit) {
        Item it = new Item();
        it.setRaw(raw);
        it.setProductQuery(productQuery);
        it.setProductId(null);
        it.setQty(qty);
        it.setUnit(unit);
        return it;
    }

    private String normalizeUnit(String u) {
        if (u == null) return null;
        String lower = u.toLowerCase(Locale.ROOT).trim();
        if (lower.equals("kilo")) return "kg"; // normalize kilo -> kg
        if (lower.equals("kg") || lower.equals("koli") || lower.equals("adet")) return lower;
        return null;
    }

    private String extractUnit(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("koli")) return "koli";
        if (lower.contains("kg")) return "kg";
        if (lower.contains("kilo")) return "kg";
        if (lower.contains("adet")) return "adet";
        return null;
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String normalizeProductQuery(String text) {
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\d+", "")
                .replace("koli", "")
                .replace("kg", "")
                .replace("kilo", "")
                .replace("adet", "")
                .replace("sepete", "")
                .replace("ekle", "")
                .replace("almak", "")
                .replace("istiyorum", "")
                .replace("alabilir", "")
                .replace("miyim", "")
                .replace("mıyım", "")
                .replace("olabilir", "")
                .replace("olur", "")
                .replace("var", "")
                .replace("mı", "")
                .replace("mi", "")
                .replace("ve", "")
                .replace("çıkar", "")
                .replace("sil", "")
                .replace("kaldır", "")
                .replace("güncelle", "")
                .replace("değiştir", "")
                .replace("düzenle", "")
                .replace("daha", "")
                .replace("fazla", "")
                .replace("azalt", "")
                .replace("artır", "")
                .replace("miktar", "")
                .replace("kaç", "")
                .replace("adet", "")
                .replace("artık", "")
                .replace("istemiyorum", "")
                .replace("vazgeçtim", "")
                .replace("temizle", "")
                .replace("boşalt", "")
                .replace("sıfırla", "")
                .replace("her", "")
                .replace("şeyi", "")
                .trim();
        return cleaned;
    }
}
