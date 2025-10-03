package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.util.LoggerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * WhatsApp Business API webhook routing service
 * Routes incoming messages to correct supplier based on phone_number_id
 */
@Service
public class WhatsAppRoutingService {
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private MarketService marketService;
    
    private static final LoggerUtility log = new LoggerUtility();
    
    /**
     * Route WhatsApp webhook to correct supplier based on phone_number_id
     * @param phoneNumberId WhatsApp Business API phone number ID
     * @param customerPhone Customer phone number from webhook
     * @return Supplier if found, null otherwise
     */
    public Supplier routeToSupplier(String phoneNumberId, String customerPhone) {
        try {
            log.logInfo("Routing WhatsApp webhook - phone_number_id: " + phoneNumberId + ", customer_phone: " + customerPhone, 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
            
            // Find supplier by phone_number_id
            Optional<Supplier> supplierOpt = supplierService.findByPhoneNumberId(phoneNumberId);
            
            if (supplierOpt.isEmpty()) {
                log.logWarn("No supplier found for phone_number_id: " + phoneNumberId, 
                    LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
                return null;
            }
            
            Supplier supplier = supplierOpt.get();
            log.logInfo("Successfully routed to supplier: " + supplier.getCompanyName() + " (ID: " + supplier.getId() + ")", 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
            
            return supplier;
            
        } catch (Exception e) {
            log.logError("Error routing WhatsApp webhook: " + e.getMessage(), 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"), e);
            return null;
        }
    }
    
    /**
     * Find market by customer phone number
     * @param customerPhone Customer phone number
     * @return Market if found, null otherwise
     */
    public Market findMarketByCustomerPhone(String customerPhone) {
        try {
            log.logInfo("Finding market for customer phone: " + customerPhone, 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
            
            Optional<Market> marketOpt = marketService.findByPhoneNormalized(customerPhone);
            
            if (marketOpt.isEmpty()) {
                log.logWarn("No market found for customer phone: " + customerPhone, 
                    LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
                return null;
            }
            
            Market market = marketOpt.get();
            log.logInfo("Successfully found market: " + market.getName() + " (ID: " + market.getId() + ")", 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
            
            return market;
            
        } catch (Exception e) {
            log.logError("Error finding market for customer phone: " + customerPhone + " - " + e.getMessage(), 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"), e);
            return null;
        }
    }
    
    /**
     * Validate webhook routing - check if supplier and market exist
     * @param phoneNumberId WhatsApp phone number ID
     * @param customerPhone Customer phone number
     * @return true if both supplier and market exist, false otherwise
     */
    public boolean validateRouting(String phoneNumberId, String customerPhone) {
        Supplier supplier = routeToSupplier(phoneNumberId, customerPhone);
        Market market = findMarketByCustomerPhone(customerPhone);
        
        boolean isValid = supplier != null && market != null;
        
        if (!isValid) {
            log.logWarn("Invalid routing - supplier: " + (supplier != null ? "found" : "not found") + 
                ", market: " + (market != null ? "found" : "not found"), 
                LoggerUtility.LogContext.create("WHATSAPP_ROUTING"));
        }
        
        return isValid;
    }
    
    /**
     * Get routing info for debugging
     * @param phoneNumberId WhatsApp phone number ID
     * @param customerPhone Customer phone number
     * @return Routing info string
     */
    public String getRoutingInfo(String phoneNumberId, String customerPhone) {
        Supplier supplier = routeToSupplier(phoneNumberId, customerPhone);
        Market market = findMarketByCustomerPhone(customerPhone);
        
        StringBuilder info = new StringBuilder();
        info.append("WhatsApp Routing Info:\n");
        info.append("Phone Number ID: ").append(phoneNumberId).append("\n");
        info.append("Customer Phone: ").append(customerPhone).append("\n");
        info.append("Supplier: ").append(supplier != null ? 
            supplier.getCompanyName() + " (ID: " + supplier.getId() + ")" : "Not found").append("\n");
        info.append("Market: ").append(market != null ? 
            market.getName() + " (ID: " + market.getId() + ")" : "Not found");
        
        return info.toString();
    }
}
