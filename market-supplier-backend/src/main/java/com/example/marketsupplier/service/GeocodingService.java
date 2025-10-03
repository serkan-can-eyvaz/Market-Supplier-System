package com.example.marketsupplier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {
    
    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&q={query}&limit=1";
    
    public static class Coordinates {
        private final double latitude;
        private final double longitude;
        
        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
    
    /**
     * Adres için koordinat bilgisi döndürür
     * @param address Aranacak adres
     * @return Koordinat bilgisi veya null
     */
    public Coordinates geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("Empty address provided for geocoding");
            return null;
        }
        
        try {
            // User-Agent header ekliyoruz (Nominatim gerekliliği)
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "MarketSupplierApp/1.0 (market-automation@example.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                NOMINATIM_URL, 
                HttpMethod.GET, 
                entity, 
                String.class, 
                address.trim()
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode results = objectMapper.readTree(response.getBody());
                
                if (results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    double lat = firstResult.get("lat").asDouble();
                    double lon = firstResult.get("lon").asDouble();
                    
                    log.info("Successfully geocoded address '{}' to coordinates ({}, {})", 
                        address, lat, lon);
                    return new Coordinates(lat, lon);
                }
            }
            
            log.warn("No geocoding results found for address: {}", address);
            return null;
            
        } catch (Exception e) {
            log.error("Error geocoding address '{}': {}", address, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * İki koordinat arasındaki mesafeyi hesaplar (Haversine formülü, km cinsinden)
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
