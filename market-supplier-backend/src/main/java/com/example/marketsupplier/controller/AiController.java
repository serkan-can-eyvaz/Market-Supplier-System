package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.AiUserPreferenceDto;
import com.example.marketsupplier.dto.AiProductPopularityDto;
import com.example.marketsupplier.dto.AiUserHistoryDto;
import com.example.marketsupplier.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Autowired
    private AiService aiService;

    @GetMapping("/user-preferences/{phone}")
    public ResponseEntity<?> getUserPreferences(@PathVariable String phone) {
        try {
            AiUserPreferenceDto preferences = aiService.getUserPreferences(phone);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Kullanıcı tercihleri alınamadı: " + e.getMessage()));
        }
    }

    @PostMapping("/user-preferences")
    public ResponseEntity<?> saveUserPreference(@RequestBody AiUserPreferenceDto preference) {
        try {
            aiService.saveUserPreference(preference);
            return ResponseEntity.ok(new MessageResponse("Kullanıcı tercihi kaydedildi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Kullanıcı tercihi kaydedilemedi: " + e.getMessage()));
        }
    }

    @GetMapping("/product-popularity")
    public ResponseEntity<?> getProductPopularity() {
        try {
            List<AiProductPopularityDto> popularity = aiService.getProductPopularity();
            return ResponseEntity.ok(popularity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Ürün popülerlik verileri alınamadı: " + e.getMessage()));
        }
    }

    @GetMapping("/user-history/{phone}")
    public ResponseEntity<?> getUserHistory(@PathVariable String phone, @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AiUserHistoryDto> history = aiService.getUserHistory(phone, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Kullanıcı geçmişi alınamadı: " + e.getMessage()));
        }
    }

    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }

    public static class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
