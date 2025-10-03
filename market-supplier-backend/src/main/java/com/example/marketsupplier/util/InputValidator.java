package com.example.marketsupplier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Component
public class InputValidator {

    private static final Logger log = LoggerFactory.getLogger(InputValidator.class);

    // Regex patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,()]+$");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern UNIT_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    
    // XSS patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONCLICK_PATTERN = Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    
    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("(--|#|/\\*|\\*/)");
    private static final Pattern SQL_QUOTE_PATTERN = Pattern.compile("['\";]");

    public static class ValidationResult {
        private final boolean valid;
        private final String sanitizedValue;
        private final List<String> errors;
        private final String originalValue;

        public ValidationResult(boolean valid, String sanitizedValue, List<String> errors, String originalValue) {
            this.valid = valid;
            this.sanitizedValue = sanitizedValue;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.originalValue = originalValue;
        }

        public boolean isValid() { return valid; }
        public String getSanitizedValue() { return sanitizedValue; }
        public List<String> getErrors() { return errors; }
        public String getOriginalValue() { return originalValue; }
        
        public void addError(String error) {
            this.errors.add(error);
        }
    }

    public ValidationResult validateAndSanitizeText(String input, String fieldName, int maxLength) {
        List<String> errors = new ArrayList<>();
        String sanitized = input;

        // Null check
        if (input == null) {
            errors.add(fieldName + " cannot be null");
            return new ValidationResult(false, null, errors, input);
        }

        // Trim and check empty
        sanitized = input.trim();
        if (sanitized.isEmpty()) {
            errors.add(fieldName + " cannot be empty");
            return new ValidationResult(false, "", errors, input);
        }

        // Length check
        if (sanitized.length() > maxLength) {
            errors.add(fieldName + " exceeds maximum length of " + maxLength);
            sanitized = sanitized.substring(0, maxLength);
        }

        // XSS protection
        String xssSanitized = sanitizeXSS(sanitized);
        if (!xssSanitized.equals(sanitized)) {
            errors.add(fieldName + " contains potentially dangerous content");
            sanitized = xssSanitized;
        }

        // SQL injection protection
        if (containsSQLInjection(sanitized)) {
            errors.add(fieldName + " contains potentially dangerous SQL content");
            sanitized = sanitizeSQLInjection(sanitized);
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, input);
    }

    public ValidationResult validatePhoneNumber(String phone) {
        List<String> errors = new ArrayList<>();
        String sanitized = phone;

        if (phone == null) {
            errors.add("Phone number cannot be null");
            return new ValidationResult(false, null, errors, phone);
        }

        sanitized = phone.trim();
        if (sanitized.isEmpty()) {
            errors.add("Phone number cannot be empty");
            return new ValidationResult(false, "", errors, phone);
        }

        // Remove spaces and special characters except +
        sanitized = sanitized.replaceAll("[\\s\\-\\(\\)]", "");
        
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            errors.add("Invalid phone number format");
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, phone);
    }

    public ValidationResult validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        String sanitized = email;

        if (email == null) {
            errors.add("Email cannot be null");
            return new ValidationResult(false, null, errors, email);
        }

        sanitized = email.trim().toLowerCase();
        if (sanitized.isEmpty()) {
            errors.add("Email cannot be empty");
            return new ValidationResult(false, "", errors, email);
        }

        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            errors.add("Invalid email format");
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, email);
    }

    public ValidationResult validateProductName(String productName) {
        List<String> errors = new ArrayList<>();
        String sanitized = productName;

        if (productName == null) {
            errors.add("Product name cannot be null");
            return new ValidationResult(false, null, errors, productName);
        }

        sanitized = productName.trim();
        if (sanitized.isEmpty()) {
            errors.add("Product name cannot be empty");
            return new ValidationResult(false, "", errors, productName);
        }

        if (sanitized.length() > 100) {
            errors.add("Product name exceeds maximum length of 100");
            sanitized = sanitized.substring(0, 100);
        }

        if (!PRODUCT_NAME_PATTERN.matcher(sanitized).matches()) {
            errors.add("Product name contains invalid characters");
        }

        // XSS protection
        String xssSanitized = sanitizeXSS(sanitized);
        if (!xssSanitized.equals(sanitized)) {
            errors.add("Product name contains potentially dangerous content");
            sanitized = xssSanitized;
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, productName);
    }

    public ValidationResult validateQuantity(String quantity) {
        List<String> errors = new ArrayList<>();
        String sanitized = quantity;

        if (quantity == null) {
            errors.add("Quantity cannot be null");
            return new ValidationResult(false, null, errors, quantity);
        }

        sanitized = quantity.trim();
        if (sanitized.isEmpty()) {
            errors.add("Quantity cannot be empty");
            return new ValidationResult(false, "", errors, quantity);
        }

        if (!QUANTITY_PATTERN.matcher(sanitized).matches()) {
            errors.add("Invalid quantity format");
            return new ValidationResult(false, sanitized, errors, quantity);
        }

        try {
            double qty = Double.parseDouble(sanitized);
            if (qty <= 0) {
                errors.add("Quantity must be greater than 0");
            }
            if (qty > 1000) {
                errors.add("Quantity exceeds maximum value of 1000");
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid quantity format");
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, quantity);
    }

    public ValidationResult validateUnit(String unit) {
        List<String> errors = new ArrayList<>();
        String sanitized = unit;

        if (unit == null) {
            errors.add("Unit cannot be null");
            return new ValidationResult(false, null, errors, unit);
        }

        sanitized = unit.trim().toLowerCase();
        if (sanitized.isEmpty()) {
            errors.add("Unit cannot be empty");
            return new ValidationResult(false, "", errors, unit);
        }

        if (!UNIT_PATTERN.matcher(sanitized).matches()) {
            errors.add("Unit contains invalid characters");
        }

        if (sanitized.length() > 20) {
            errors.add("Unit exceeds maximum length of 20");
            sanitized = sanitized.substring(0, 20);
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, unit);
    }

    public ValidationResult validateMessage(String message) {
        return validateAndSanitizeText(message, "Message", 1000);
    }

    public ValidationResult validateAddress(String address) {
        return validateAndSanitizeText(address, "Address", 200);
    }

    public ValidationResult validateName(String name) {
        return validateAndSanitizeText(name, "Name", 50);
    }

    public ValidationResult validateDescription(String description) {
        return validateAndSanitizeText(description, "Description", 500);
    }

    public ValidationResult validatePrice(String price) {
        List<String> errors = new ArrayList<>();
        String sanitized = price;

        if (price == null) {
            errors.add("Price cannot be null");
            return new ValidationResult(false, null, errors, price);
        }

        sanitized = price.trim();
        if (sanitized.isEmpty()) {
            errors.add("Price cannot be empty");
            return new ValidationResult(false, "", errors, price);
        }

        try {
            double priceValue = Double.parseDouble(sanitized);
            if (priceValue < 0) {
                errors.add("Price cannot be negative");
            }
            if (priceValue > 100000) {
                errors.add("Price exceeds maximum value of 100000");
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid price format");
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, price);
    }

    public Map<String, ValidationResult> validateWhatsAppMessage(Map<String, Object> messageData) {
        Map<String, ValidationResult> results = new HashMap<>();
        
        // Validate phone number
        String from = (String) messageData.get("from");
        results.put("from", validatePhoneNumber(from));
        
        // Validate message text
        String text = (String) messageData.get("text");
        results.put("text", validateMessage(text));
        
        // Validate message type
        String type = (String) messageData.get("type");
        results.put("type", validateMessageType(type));
        
        return results;
    }

    public ValidationResult validateMessageType(String type) {
        List<String> errors = new ArrayList<>();
        String sanitized = type;

        if (type == null) {
            errors.add("Message type cannot be null");
            return new ValidationResult(false, null, errors, type);
        }

        sanitized = type.trim().toLowerCase();
        if (sanitized.isEmpty()) {
            errors.add("Message type cannot be empty");
            return new ValidationResult(false, "", errors, type);
        }

        if (!sanitized.matches("^(text|image|document|audio|video)$")) {
            errors.add("Invalid message type");
        }

        return new ValidationResult(errors.isEmpty(), sanitized, errors, type);
    }

    private String sanitizeXSS(String input) {
        if (input == null) return null;
        
        String sanitized = input;
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove event handlers
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONCLICK_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // HTML encode special characters
        sanitized = sanitized.replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;")
                           .replace("&", "&amp;");
        
        return sanitized;
    }

    private boolean containsSQLInjection(String input) {
        if (input == null) return false;
        
        String lowerInput = input.toLowerCase();
        return SQL_INJECTION_PATTERN.matcher(lowerInput).find() ||
               SQL_COMMENT_PATTERN.matcher(lowerInput).find() ||
               SQL_QUOTE_PATTERN.matcher(lowerInput).find();
    }

    private String sanitizeSQLInjection(String input) {
        if (input == null) return null;
        
        String sanitized = input;
        
        // Remove SQL keywords
        sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove SQL comments
        sanitized = SQL_COMMENT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Escape quotes
        sanitized = sanitized.replace("'", "''")
                           .replace("\"", "\\\"")
                           .replace(";", "\\;");
        
        return sanitized;
    }

    public boolean isAllValid(Map<String, ValidationResult> results) {
        return results.values().stream().allMatch(ValidationResult::isValid);
    }

    public List<String> getAllErrors(Map<String, ValidationResult> results) {
        List<String> allErrors = new ArrayList<>();
        results.values().forEach(result -> allErrors.addAll(result.getErrors()));
        return allErrors;
    }

    public String getSanitizedValue(Map<String, ValidationResult> results, String fieldName) {
        ValidationResult result = results.get(fieldName);
        return result != null ? result.getSanitizedValue() : null;
    }
}
