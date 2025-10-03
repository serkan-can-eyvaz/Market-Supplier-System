package com.example.marketsupplier.agent.nlp;

import com.example.marketsupplier.agent.model.NluResult;
import com.example.marketsupplier.service.CustomerContext;

public interface IntentExtractor {
    NluResult extract(String text);
    default NluResult extract(String text, CustomerContext context) {
        return extract(text); // Backward compatibility
    }
}
