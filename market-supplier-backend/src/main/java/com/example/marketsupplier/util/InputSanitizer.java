package com.example.marketsupplier.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|onclick)",
        Pattern.CASE_INSENSITIVE
    );

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload|onerror|onclick|onmouseover|onfocus|onblur|onchange|onsubmit|onreset|onselect|onkeydown|onkeyup|onkeypress)",
        Pattern.CASE_INSENSITIVE
    );

    // HTML tags
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "<[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    // Special characters that might cause issues
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(
        "[<>\"'&]",
        Pattern.CASE_INSENSITIVE
    );

    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }

        // Remove null bytes
        String sanitized = input.replaceAll("\0", "");

        // Trim whitespace
        sanitized = sanitized.trim();

        // Check for SQL injection
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Potential SQL injection detected");
        }

        // Check for XSS
        if (XSS_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Potential XSS attack detected");
        }

        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Escape special characters
        sanitized = escapeSpecialCharacters(sanitized);

        return sanitized;
    }

    public String sanitizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        // Remove all non-digit characters except +
        String sanitized = phone.replaceAll("[^0-9+]", "");

        // Validate phone format
        if (!sanitized.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new SecurityException("Invalid phone number format");
        }

        return sanitized;
    }

    public String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }

        // Basic sanitization
        String sanitized = sanitizeText(message);

        // Limit length
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }

        return sanitized;
    }

    public String sanitizeProductName(String productName) {
        if (productName == null) {
            return null;
        }

        // Basic sanitization
        String sanitized = sanitizeText(productName);

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    public String sanitizeQuantity(String quantity) {
        if (quantity == null) {
            return null;
        }

        // Remove all non-digit characters
        String sanitized = quantity.replaceAll("[^0-9]", "");

        // Validate quantity
        if (sanitized.isEmpty() || !sanitized.matches("^[1-9]\\d{0,2}$")) {
            throw new SecurityException("Invalid quantity format");
        }

        return sanitized;
    }

    public String sanitizeUnit(String unit) {
        if (unit == null) {
            return null;
        }

        // Basic sanitization
        String sanitized = sanitizeText(unit);

        // Limit length
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }

        return sanitized;
    }

    private String escapeSpecialCharacters(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public boolean isValidInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        try {
            sanitizeText(input);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }
}
