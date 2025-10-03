package com.example.marketsupplier.agent.util;

import org.springframework.stereotype.Component;
import java.text.Normalizer;

@Component
public class StringNormalizer {

    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        // Step 1: Lowercase
        String lowercased = input.toLowerCase();
        
        // Step 2: Handle special Turkish characters
        lowercased = lowercased.replaceAll("ı", "i");
        lowercased = lowercased.replaceAll("ç", "c");
        lowercased = lowercased.replaceAll("ş", "s");
        lowercased = lowercased.replaceAll("ğ", "g");
        lowercased = lowercased.replaceAll("ü", "u");
        lowercased = lowercased.replaceAll("ö", "o");

        // Step 3: Normalize to NFD (canonical decomposition), which separates diacritics
        String nfdNormalizedString = Normalizer.normalize(lowercased, Normalizer.Form.NFD);
        
        // Step 4: Remove diacritics (accents, etc.)
        String withoutDiacritics = nfdNormalizedString.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // Step 5: Remove non-alphanumeric characters for cleaner matching, but keep spaces
        String cleaned = withoutDiacritics.replaceAll("[^a-z0-9\\s]", "").trim();
        
        return cleaned;
    }
    
    public String normalizeForProductMatching(String input) {
        if (input == null) {
            return "";
        }
        
        // Sadece temel normalizasyon yap, AI kendisi anlayacak
        return normalize(input);
    }
}
