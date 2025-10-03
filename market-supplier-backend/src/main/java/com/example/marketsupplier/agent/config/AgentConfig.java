package com.example.marketsupplier.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {
    
    private Nlu nlu = new Nlu();
    private Reservation reservation = new Reservation();
    private double bigOrderThreshold = 10000.00;
    private Llm llm = new Llm();
    
    public Nlu getNlu() { return nlu; }
    public void setNlu(Nlu nlu) { this.nlu = nlu; }
    
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    
    public double getBigOrderThreshold() { return bigOrderThreshold; }
    public void setBigOrderThreshold(double bigOrderThreshold) { this.bigOrderThreshold = bigOrderThreshold; }
    
    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    
    public static class Nlu {
        private double confidenceThreshold = 0.75;
        
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    }
    
    public static class Reservation {
        private int ttlMinutes = 15;
        
        public int getTtlMinutes() { return ttlMinutes; }
        public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }
    }
    
    public static class Llm {
        private boolean enabled = false;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
