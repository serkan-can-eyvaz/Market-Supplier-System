package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.repository.MarketRepository;
import com.example.marketsupplier.service.GeocodingService.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MarketService {
    
    private static final Logger log = LoggerFactory.getLogger(MarketService.class);
    
    @Autowired
    private MarketRepository marketRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GeocodingService geocodingService;
    
    // Create new market
    public Market createMarket(Long userId, String name, String address, String phone) {
        // Get user
        User user = userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Tedarikçinin market oluşturmasına izin ver
        if (user.getRole() != UserRole.SUPPLIER && user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Only SUPPLIER or ADMIN can create a market");
        }
        
        // Çoklu market desteği: aynı kullanıcı birden fazla market oluşturabilir
        
        // Check if phone already exists
        if (marketRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Phone number already exists: " + phone);
        }
        
        // Create market
        Market market = new Market(user, name, address, phone);
        
        // Automatically geocode the address
        try {
            Coordinates coords = geocodingService.geocodeAddress(address);
            if (coords != null) {
                market.setLatitude(coords.getLatitude());
                market.setLongitude(coords.getLongitude());
                log.info("Market '{}' geocoded to coordinates ({}, {})", 
                    name, coords.getLatitude(), coords.getLongitude());
            } else {
                log.warn("Failed to geocode address for market '{}': {}", name, address);
            }
        } catch (Exception e) {
            log.error("Error during geocoding for market '{}': {}", name, e.getMessage());
        }
        
        return marketRepository.save(market);
    }
    
    // Find market by ID
    public Optional<Market> findById(Long id) {
        return marketRepository.findById(id);
    }
    
    // Find market by user
    public Optional<Market> findByUser(User user) {
        return marketRepository.findByUser(user);
    }
    
    // Find market by user ID
    public Optional<Market> findByUserId(Long userId) {
        return marketRepository.findByUserId(userId);
    }

    // Find all markets by user ID
    public List<Market> findAllByUserId(Long userId) {
        return marketRepository.findAllByUserId(userId);
    }
    
    // Find all markets by user ID with pagination
    public Page<Market> findAllByUserIdPaginated(Long userId, Pageable pageable) {
        return marketRepository.findAllByUserId(userId, pageable);
    }
    
    // Get all markets
    public List<Market> getAllMarkets() {
        return marketRepository.findAllByOrderByCreatedAtDesc();
    }

    // Find by phone with basic normalization (digits only)
    public Optional<Market> findByPhoneNormalized(String phone) {
        if (phone == null) return Optional.empty();
        String target = normalizePhone(phone);
        System.out.println("DEBUG: Looking for phone: " + phone + " -> normalized: " + target);
        
        List<Market> allMarkets = marketRepository.findAll();
        System.out.println("DEBUG: Total markets in DB: " + allMarkets.size());
        
        for (Market market : allMarkets) {
            String marketPhone = normalizePhone(market.getPhone());
            System.out.println("DEBUG: Market " + market.getId() + " phone: " + market.getPhone() + " -> normalized: " + marketPhone);
            if (marketPhone.equals(target)) {
                System.out.println("DEBUG: Found matching market: " + market.getId());
                return Optional.of(market);
            }
        }
        
        System.out.println("DEBUG: No matching market found");
        return Optional.empty();
    }

    private String normalizePhone(String input) {
        if (input == null) return "";
        return input.replaceAll("[^0-9]", "");
    }
    
    // Search markets by name
    public List<Market> searchMarketsByName(String name) {
        return marketRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Search markets by address
    public List<Market> searchMarketsByAddress(String address) {
        return marketRepository.findByAddressContainingIgnoreCase(address);
    }
    
    // Update market
    public Market updateMarket(Long id, String name, String address, String phone) {
        Market market = marketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Market not found with id: " + id));
        
        // Check if phone is being changed and if new phone already exists
        if (!market.getPhone().equals(phone)) {
            Optional<Market> existingMarket = marketRepository.findByPhone(phone);
            if (existingMarket.isPresent() && !existingMarket.get().getId().equals(id)) {
                throw new RuntimeException("Phone number already exists: " + phone);
            }
        }
        
        // Check if address changed
        boolean addressChanged = !market.getAddress().equals(address);
        
        market.setName(name);
        market.setAddress(address);
        market.setPhone(phone);
        
        // Re-geocode if address changed
        if (addressChanged) {
            try {
                Coordinates coords = geocodingService.geocodeAddress(address);
                if (coords != null) {
                    market.setLatitude(coords.getLatitude());
                    market.setLongitude(coords.getLongitude());
                    log.info("Market '{}' re-geocoded to coordinates ({}, {})", 
                        name, coords.getLatitude(), coords.getLongitude());
                } else {
                    log.warn("Failed to re-geocode address for market '{}': {}", name, address);
                }
            } catch (Exception e) {
                log.error("Error during re-geocoding for market '{}': {}", name, e.getMessage());
            }
        }
        
        return marketRepository.save(market);
    }
    
    // Delete market (siparişlerle birlikte)
    public void deleteMarket(Long id) {
        Market market = marketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Market not found with id: " + id));
        
        // Market silinirken siparişleri de otomatik silinir (cascade = CascadeType.ALL)
        log.info("Deleting market '{}' with {} orders", market.getName(), 
            market.getOrders() != null ? market.getOrders().size() : 0);
        
        marketRepository.delete(market);
    }
    
    // Get market statistics
    public MarketStats getMarketStats() {
        long totalMarkets = marketRepository.count();
        List<Object[]> marketsWithOrderCounts = marketRepository.findMarketsWithOrderCounts();
        
        return new MarketStats(totalMarkets, marketsWithOrderCounts);
    }
    
    // Check if market exists for user
    public boolean marketExistsForUser(Long userId) {
        return marketRepository.existsByUserId(userId);
    }
    
    // Get market by user email
    public Optional<Market> findByUserEmail(String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            return marketRepository.findByUser(user.get());
        }
        return Optional.empty();
    }
    
    // Validate market ownership
    public boolean isMarketOwner(Long marketId, Long userId) {
        Optional<Market> market = marketRepository.findById(marketId);
        return market.isPresent() && market.get().getUser().getId().equals(userId);
    }
    
    // Manual coordinate update for existing markets
    public Market updateMarketCoordinates(Long marketId) {
        Market market = marketRepository.findById(marketId)
            .orElseThrow(() -> new RuntimeException("Market not found with id: " + marketId));
        
        try {
            Coordinates coords = geocodingService.geocodeAddress(market.getAddress());
            if (coords != null) {
                market.setLatitude(coords.getLatitude());
                market.setLongitude(coords.getLongitude());
                log.info("Market '{}' coordinates updated to ({}, {})", 
                    market.getName(), coords.getLatitude(), coords.getLongitude());
                return marketRepository.save(market);
            } else {
                throw new RuntimeException("Failed to geocode address: " + market.getAddress());
            }
        } catch (Exception e) {
            log.error("Error updating coordinates for market '{}': {}", market.getName(), e.getMessage());
            throw new RuntimeException("Failed to update coordinates: " + e.getMessage());
        }
    }
    
    // Save market (for coordinate updates via WhatsApp)
    public Market saveMarket(Market market) {
        return marketRepository.save(market);
    }
    
    // Inner class for market statistics
    public static class MarketStats {
        private final long totalMarkets;
        private final List<Object[]> marketsWithOrderCounts;
        
        public MarketStats(long totalMarkets, List<Object[]> marketsWithOrderCounts) {
            this.totalMarkets = totalMarkets;
            this.marketsWithOrderCounts = marketsWithOrderCounts;
        }
        
        // Getters
        public long getTotalMarkets() { return totalMarkets; }
        public List<Object[]> getMarketsWithOrderCounts() { return marketsWithOrderCounts; }
    }
}
