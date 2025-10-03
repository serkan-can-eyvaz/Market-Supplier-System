package com.example.marketsupplier.security;

import com.example.marketsupplier.util.LoggerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PIIService {

    @Autowired
    private LoggerUtility loggerUtility;

    @Autowired
    private AuditLogService auditLogService;

    // PII Patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[1-9]\\d{1,14}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3[0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_PHONE")
            .withMetadata("original_length", phoneNumber.length());

        String masked = maskPhoneNumberInternal(phoneNumber);
        
        auditLogService.logPIIAccess("system", "phone_number", "mask", phoneNumber, "success");
        loggerUtility.logSecurity("Phone number masked", context, Map.of("operation", "mask_phone"));
        
        return masked;
    }

    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_EMAIL")
            .withMetadata("original_length", email.length());

        String masked = maskEmailInternal(email);
        
        auditLogService.logPIIAccess("system", "email", "mask", email, "success");
        loggerUtility.logSecurity("Email masked", context, Map.of("operation", "mask_email"));
        
        return masked;
    }

    public String maskCreditCard(String creditCard) {
        if (creditCard == null || creditCard.trim().isEmpty()) {
            return creditCard;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_CREDIT_CARD")
            .withMetadata("original_length", creditCard.length());

        String masked = maskCreditCardInternal(creditCard);
        
        auditLogService.logPIIAccess("system", "credit_card", "mask", creditCard, "success");
        loggerUtility.logSecurity("Credit card masked", context, Map.of("operation", "mask_credit_card"));
        
        return masked;
    }

    public String maskSSN(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return ssn;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_SSN")
            .withMetadata("original_length", ssn.length());

        String masked = maskSSNInternal(ssn);
        
        auditLogService.logPIIAccess("system", "ssn", "mask", ssn, "success");
        loggerUtility.logSecurity("SSN masked", context, Map.of("operation", "mask_ssn"));
        
        return masked;
    }

    public String maskIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddress;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_IP")
            .withMetadata("original_length", ipAddress.length());

        String masked = maskIPAddressInternal(ipAddress);
        
        auditLogService.logPIIAccess("system", "ip_address", "mask", ipAddress, "success");
        loggerUtility.logSecurity("IP address masked", context, Map.of("operation", "mask_ip"));
        
        return masked;
    }

    public String maskText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_TEXT")
            .withMetadata("original_length", text.length());

        String masked = text;
        
        // Mask phone numbers
        masked = PHONE_PATTERN.matcher(masked).replaceAll(matchResult -> 
            maskPhoneNumberInternal(matchResult.group()));
        
        // Mask emails
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(matchResult -> 
            maskEmailInternal(matchResult.group()));
        
        // Mask credit cards
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll(matchResult -> 
            maskCreditCardInternal(matchResult.group()));
        
        // Mask SSNs
        masked = SSN_PATTERN.matcher(masked).replaceAll(matchResult -> 
            maskSSNInternal(matchResult.group()));
        
        // Mask IP addresses
        masked = IP_PATTERN.matcher(masked).replaceAll(matchResult -> 
            maskIPAddressInternal(matchResult.group()));
        
        auditLogService.logPIIAccess("system", "text", "mask", text, "success");
        loggerUtility.logSecurity("Text masked for PII", context, Map.of("operation", "mask_text"));
        
        return masked;
    }

    public Map<String, Object> maskObject(Map<String, Object> data) {
        if (data == null) {
            return data;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_MASK_OBJECT")
            .withMetadata("field_count", data.size());

        Map<String, Object> maskedData = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                if (isPIIField(key)) {
                    maskedData.put(key, maskText(stringValue));
                } else {
                    maskedData.put(key, stringValue);
                }
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                maskedData.put(key, maskObject(nestedMap));
            } else {
                maskedData.put(key, value);
            }
        }
        
        auditLogService.logPIIAccess("system", "object", "mask", "object", "success");
        loggerUtility.logSecurity("Object masked for PII", context, Map.of("operation", "mask_object"));
        
        return maskedData;
    }

    public boolean containsPII(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return PHONE_PATTERN.matcher(text).find() ||
               EMAIL_PATTERN.matcher(text).find() ||
               CREDIT_CARD_PATTERN.matcher(text).find() ||
               SSN_PATTERN.matcher(text).find() ||
               IP_PATTERN.matcher(text).find();
    }

    public Map<String, Boolean> detectPII(String text) {
        Map<String, Boolean> detection = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            detection.put("phone", false);
            detection.put("email", false);
            detection.put("creditCard", false);
            detection.put("ssn", false);
            detection.put("ipAddress", false);
            return detection;
        }

        detection.put("phone", PHONE_PATTERN.matcher(text).find());
        detection.put("email", EMAIL_PATTERN.matcher(text).find());
        detection.put("creditCard", CREDIT_CARD_PATTERN.matcher(text).find());
        detection.put("ssn", SSN_PATTERN.matcher(text).find());
        detection.put("ipAddress", IP_PATTERN.matcher(text).find());
        
        return detection;
    }

    public String anonymizeForLogging(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_ANONYMIZE")
            .withMetadata("original_length", text.length());

        String anonymized = maskText(text);
        
        auditLogService.logPIIAccess("system", "text", "anonymize", text, "success");
        loggerUtility.logSecurity("Text anonymized for logging", context, Map.of("operation", "anonymize_text"));
        
        return anonymized;
    }

    public String hashForStorage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("PII_HASH")
            .withMetadata("original_length", text.length());

        String hashed = hashText(text);
        
        auditLogService.logPIIAccess("system", "text", "hash", text, "success");
        loggerUtility.logSecurity("Text hashed for storage", context, Map.of("operation", "hash_text"));
        
        return hashed;
    }

    private String maskPhoneNumberInternal(String phoneNumber) {
        if (phoneNumber.length() <= 4) {
            return "*".repeat(phoneNumber.length());
        }
        
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() <= 4) {
            return "*".repeat(phoneNumber.length());
        }
        
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }

    private String maskEmailInternal(String email) {
        if (!email.contains("@")) {
            return "*".repeat(email.length());
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            localPart = "*".repeat(localPart.length());
        } else {
            localPart = localPart.charAt(0) + "*".repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1);
        }
        
        return localPart + "@" + domain;
    }

    private String maskCreditCardInternal(String creditCard) {
        String cleaned = creditCard.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) {
            return "*".repeat(creditCard.length());
        }
        
        return "****-****-****-" + cleaned.substring(cleaned.length() - 4);
    }

    private String maskSSNInternal(String ssn) {
        String cleaned = ssn.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) {
            return "*".repeat(ssn.length());
        }
        
        return "***-**-" + cleaned.substring(cleaned.length() - 4);
    }

    private String maskIPAddressInternal(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return "*".repeat(ipAddress.length());
        }
        
        return parts[0] + "." + parts[1] + ".***.***";
    }

    private boolean isPIIField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("phone") ||
               lowerFieldName.contains("email") ||
               lowerFieldName.contains("credit") ||
               lowerFieldName.contains("ssn") ||
               lowerFieldName.contains("social") ||
               lowerFieldName.contains("ip") ||
               lowerFieldName.contains("address") ||
               lowerFieldName.contains("name") ||
               lowerFieldName.contains("id");
    }

    private String hashText(String text) {
        // Simple hash implementation - in production, use proper cryptographic hash
        return "HASH_" + text.hashCode();
    }
}
