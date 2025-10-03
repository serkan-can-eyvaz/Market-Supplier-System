package com.example.marketsupplier.service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerContext implements Serializable {
    private String phone;
    private String name;
    private String currentState; // browsing, cart, ordering
    private List<String> conversationHistory = new ArrayList<>();
    private Map<String, Object> sessionData = new HashMap<>();
    private LocalDateTime lastActivity;
    private boolean hasActiveCart = false; // Aktif sepet var mı?
    private String lastCartState = ""; // Son sepet durumu

    public CustomerContext() {}

    public CustomerContext(String phone) {
        this.phone = phone;
        this.currentState = "browsing";
        this.lastActivity = LocalDateTime.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CustomerContext ctx = new CustomerContext();
        public Builder phone(String phone) { ctx.phone = phone; return this; }
        public Builder name(String name) { ctx.name = name; return this; }
        public Builder currentState(String state) { ctx.currentState = state; return this; }
        public Builder lastActivity(LocalDateTime t) { ctx.lastActivity = t; return this; }
        public CustomerContext build() { return ctx; }
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public List<String> getConversationHistory() { return conversationHistory; }
    public void addConversationLine(String line) {
        if (this.conversationHistory == null) {
            this.conversationHistory = new ArrayList<>();
        }
        this.conversationHistory.add(line);
    }
    public void setConversationHistory(List<String> conversationHistory) { this.conversationHistory = conversationHistory; }
    public Map<String, Object> getSessionData() { return sessionData; }
    public void setSessionData(Map<String, Object> sessionData) { this.sessionData = sessionData; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    public boolean isHasActiveCart() { return hasActiveCart; }
    public void setHasActiveCart(boolean hasActiveCart) { this.hasActiveCart = hasActiveCart; }
    public String getLastCartState() { return lastCartState; }
    public void setLastCartState(String lastCartState) { this.lastCartState = lastCartState; }
}


