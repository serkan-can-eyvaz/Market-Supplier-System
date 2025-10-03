package com.example.marketsupplier.service;

import com.example.marketsupplier.dto.AiUserPreferenceDto;
import com.example.marketsupplier.dto.AiProductPopularityDto;
import com.example.marketsupplier.dto.AiUserHistoryDto;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.repository.OrderRepository;
import com.example.marketsupplier.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public AiUserPreferenceDto getUserPreferences(String phone) {
        // Order entity'sinde customer phone bilgisi yok, bu yüzden boş döndürüyoruz
        // Gerçek uygulamada customer phone bilgisini Order entity'sine eklemek gerekir
        
        AiUserPreferenceDto preferences = new AiUserPreferenceDto();
        preferences.setPhone(phone);
        preferences.setFavoriteProducts(new ArrayList<>()); // Boş liste döndür
        
        return preferences;
    }

    public void saveUserPreference(AiUserPreferenceDto preference) {
        // Bu implementasyon için basit bir log sistemi kullanabiliriz
        // Gerçek uygulamada bu verileri veritabanında saklayabilirsiniz
        System.out.println("AI Learning: User " + preference.getPhone() + 
                         " selected product " + preference.getSelectedProductId() + 
                         " for query '" + preference.getProductName() + "'");
    }

    public List<AiProductPopularityDto> getProductPopularity() {
        List<Product> products = productRepository.findAll();
        List<AiProductPopularityDto> popularityList = new ArrayList<>();
        
        for (Product product : products) {
            // Order entity'sinde productName field'ı yok, bu yüzden 0 döndürüyoruz
            int orderCount = 0;
            
            // Popülerlik skorunu hesapla (basit algoritma)
            double popularityScore = calculatePopularityScore(orderCount, product.getPrice().doubleValue());
            
            AiProductPopularityDto popularity = new AiProductPopularityDto(
                product.getId(),
                product.getName(),
                popularityScore,
                orderCount,
                0 // searchCount - şimdilik 0
            );
            
            popularityList.add(popularity);
        }
        
        // Popülerlik skoruna göre sırala
        popularityList.sort((a, b) -> Double.compare(b.getPopularityScore(), a.getPopularityScore()));
        
        return popularityList;
    }

    public List<AiUserHistoryDto> getUserHistory(String phone, int limit) {
        // Order entity'sinde customer phone bilgisi yok, bu yüzden boş döndürüyoruz
        // Gerçek uygulamada customer phone bilgisini Order entity'sine eklemek gerekir
        
        return new ArrayList<>(); // Boş liste döndür
    }

    private double calculatePopularityScore(int orderCount, double price) {
        // Basit popülerlik skoru hesaplama
        // Daha fazla sipariş = daha yüksek skor
        // Daha düşük fiyat = daha yüksek skor (erişilebilirlik)
        double orderScore = Math.log(orderCount + 1) * 10; // Logaritmik artış
        double priceScore = Math.max(0, 100 - price); // Fiyat tersine çevirme
        
        return orderScore + (priceScore * 0.1); // Fiyat etkisini azalt
    }
}
