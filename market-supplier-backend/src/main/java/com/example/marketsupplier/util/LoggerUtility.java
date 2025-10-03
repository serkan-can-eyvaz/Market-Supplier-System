package com.example.marketsupplier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Component
public class LoggerUtility {

    private static final Logger log = LoggerFactory.getLogger(LoggerUtility.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${app.logging.level:INFO}")
    private String logLevel;

    @Value("${app.logging.structured:true}")
    private boolean structuredLogging;

    @Value("${app.logging.include-stack-trace:false}")
    private boolean includeStackTrace;

    @Value("${app.logging.service-name:market-supplier}")
    private String serviceName;

    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    public static class LogContext {
        private final String traceId;
        private final String userId;
        private final String sessionId;
        private final String operation;
        private final Map<String, Object> metadata;

        public LogContext(String traceId, String userId, String sessionId, String operation, Map<String, Object> metadata) {
            this.traceId = traceId;
            this.userId = userId;
            this.sessionId = sessionId;
            this.operation = operation;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public String getTraceId() { return traceId; }
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public String getOperation() { return operation; }
        public Map<String, Object> getMetadata() { return metadata; }

        public static LogContext create(String operation) {
            return new LogContext(UUID.randomUUID().toString(), null, null, operation, null);
        }

        public static LogContext create(String operation, String userId) {
            return new LogContext(UUID.randomUUID().toString(), userId, null, operation, null);
        }

        public static LogContext create(String operation, String userId, String sessionId) {
            return new LogContext(UUID.randomUUID().toString(), userId, sessionId, operation, null);
        }

        public LogContext withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public LogContext withMetadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
    }

    public void logInfo(String message, LogContext context) {
        if (shouldLog(LogLevel.INFO)) {
            logStructured(LogLevel.INFO, message, context, null);
        }
    }

    public void logInfo(String message, LogContext context, Map<String, Object> additionalData) {
        if (shouldLog(LogLevel.INFO)) {
            LogContext enrichedContext = context.withMetadata(additionalData);
            logStructured(LogLevel.INFO, message, enrichedContext, null);
        }
    }

    public void logDebug(String message, LogContext context) {
        if (shouldLog(LogLevel.DEBUG)) {
            logStructured(LogLevel.DEBUG, message, context, null);
        }
    }

    public void logDebug(String message, LogContext context, Map<String, Object> additionalData) {
        if (shouldLog(LogLevel.DEBUG)) {
            LogContext enrichedContext = context.withMetadata(additionalData);
            logStructured(LogLevel.DEBUG, message, enrichedContext, null);
        }
    }

    public void logWarn(String message, LogContext context) {
        if (shouldLog(LogLevel.WARN)) {
            logStructured(LogLevel.WARN, message, context, null);
        }
    }

    public void logWarn(String message, LogContext context, Exception exception) {
        if (shouldLog(LogLevel.WARN)) {
            logStructured(LogLevel.WARN, message, context, exception);
        }
    }

    public void logWarn(String message, LogContext context, Map<String, Object> additionalData) {
        if (shouldLog(LogLevel.WARN)) {
            LogContext enrichedContext = context.withMetadata(additionalData);
            logStructured(LogLevel.WARN, message, enrichedContext, null);
        }
    }

    public void logError(String message, LogContext context) {
        if (shouldLog(LogLevel.ERROR)) {
            logStructured(LogLevel.ERROR, message, context, null);
        }
    }

    public void logError(String message, LogContext context, Exception exception) {
        if (shouldLog(LogLevel.ERROR)) {
            logStructured(LogLevel.ERROR, message, context, exception);
        }
    }

    public void logError(String message, LogContext context, Exception exception, Map<String, Object> additionalData) {
        if (shouldLog(LogLevel.ERROR)) {
            LogContext enrichedContext = context.withMetadata(additionalData);
            logStructured(LogLevel.ERROR, message, enrichedContext, exception);
        }
    }

    public void logTrace(String message, LogContext context) {
        if (shouldLog(LogLevel.TRACE)) {
            logStructured(LogLevel.TRACE, message, context, null);
        }
    }

    public void logTrace(String message, LogContext context, Map<String, Object> additionalData) {
        if (shouldLog(LogLevel.TRACE)) {
            LogContext enrichedContext = context.withMetadata(additionalData);
            logStructured(LogLevel.TRACE, message, enrichedContext, null);
        }
    }

    public void logPerformance(String operation, long durationMs, LogContext context) {
        Map<String, Object> performanceData = Map.of(
            "operation", operation,
            "duration_ms", durationMs,
            "performance_metric", true
        );
        logInfo("Performance metric", context, performanceData);
    }

    public void logSecurity(String event, LogContext context, Map<String, Object> securityData) {
        Map<String, Object> enrichedData = new HashMap<>(securityData);
        enrichedData.put("security_event", true);
        enrichedData.put("event_type", event);
        logWarn("Security event", context, enrichedData);
    }

    public void logBusiness(String event, LogContext context, Map<String, Object> businessData) {
        Map<String, Object> enrichedData = new HashMap<>(businessData);
        enrichedData.put("business_event", true);
        enrichedData.put("event_type", event);
        logInfo("Business event", context, enrichedData);
    }

    public void logSystem(String event, LogContext context, Map<String, Object> systemData) {
        Map<String, Object> enrichedData = new HashMap<>(systemData);
        enrichedData.put("system_event", true);
        enrichedData.put("event_type", event);
        logInfo("System event", context, enrichedData);
    }

    private void logStructured(LogLevel level, String message, LogContext context, Exception exception) {
        if (structuredLogging) {
            logStructuredJson(level, message, context, exception);
        } else {
            logSimple(level, message, context, exception);
        }
    }

    private void logStructuredJson(LogLevel level, String message, LogContext context, Exception exception) {
        try {
            ObjectNode logEntry = objectMapper.createObjectNode();
            
            // Basic fields
            logEntry.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
            logEntry.put("level", level.name());
            logEntry.put("message", message);
            logEntry.put("service", serviceName);
            
            // Context fields
            if (context != null) {
                logEntry.put("trace_id", context.getTraceId());
                if (context.getUserId() != null) {
                    logEntry.put("user_id", context.getUserId());
                }
                if (context.getSessionId() != null) {
                    logEntry.put("session_id", context.getSessionId());
                }
                if (context.getOperation() != null) {
                    logEntry.put("operation", context.getOperation());
                }
                
                // Metadata
                if (!context.getMetadata().isEmpty()) {
                    ObjectNode metadataNode = objectMapper.valueToTree(context.getMetadata());
                    logEntry.set("metadata", metadataNode);
                }
            }
            
            // Exception fields
            if (exception != null) {
                logEntry.put("exception_class", exception.getClass().getSimpleName());
                logEntry.put("exception_message", exception.getMessage());
                
                if (includeStackTrace) {
                    logEntry.put("stack_trace", getStackTrace(exception));
                }
            }
            
            // Log based on level
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            switch (level) {
                case TRACE -> log.trace(jsonLog);
                case DEBUG -> log.debug(jsonLog);
                case INFO -> log.info(jsonLog);
                case WARN -> log.warn(jsonLog);
                case ERROR -> log.error(jsonLog);
            }
            
        } catch (Exception e) {
            // Fallback to simple logging if JSON serialization fails
            logSimple(level, message, context, exception);
        }
    }

    private void logSimple(LogLevel level, String message, LogContext context, Exception exception) {
        StringBuilder logMessage = new StringBuilder();
        
        if (context != null) {
            logMessage.append("[");
            logMessage.append(context.getTraceId());
            if (context.getUserId() != null) {
                logMessage.append("|").append(context.getUserId());
            }
            if (context.getOperation() != null) {
                logMessage.append("|").append(context.getOperation());
            }
            logMessage.append("] ");
        }
        
        logMessage.append(message);
        
        if (exception != null) {
            logMessage.append(" - Exception: ").append(exception.getMessage());
        }
        
        switch (level) {
            case TRACE -> log.trace(logMessage.toString(), exception);
            case DEBUG -> log.debug(logMessage.toString(), exception);
            case INFO -> log.info(logMessage.toString(), exception);
            case WARN -> log.warn(logMessage.toString(), exception);
            case ERROR -> log.error(logMessage.toString(), exception);
        }
    }

    private boolean shouldLog(LogLevel level) {
        // Null/boş/yanlış değerleri INFO'ya düşür
        String lvl = (logLevel == null || logLevel.isBlank()) ? "INFO" : logLevel;
        LogLevel currentLevel;
        try {
            currentLevel = LogLevel.valueOf(lvl.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            currentLevel = LogLevel.INFO;
        }
        return level.ordinal() >= currentLevel.ordinal();
    }

    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    // Convenience methods for common logging patterns
    public void logRequest(String method, String path, String userId, Map<String, Object> requestData) {
        LogContext context = LogContext.create("HTTP_REQUEST", userId)
            .withMetadata("method", method)
            .withMetadata("path", path)
            .withMetadata(requestData);
        logInfo("HTTP request received", context);
    }

    public void logResponse(String method, String path, String userId, int statusCode, long durationMs) {
        LogContext context = LogContext.create("HTTP_RESPONSE", userId)
            .withMetadata("method", method)
            .withMetadata("path", path)
            .withMetadata("status_code", statusCode);
        logPerformance("HTTP_RESPONSE", durationMs, context);
    }

    public void logDatabaseOperation(String operation, String table, String userId, long durationMs) {
        LogContext context = LogContext.create("DATABASE_OPERATION", userId)
            .withMetadata("operation", operation)
            .withMetadata("table", table);
        logPerformance("DATABASE_OPERATION", durationMs, context);
    }

    public void logExternalServiceCall(String service, String endpoint, String userId, int statusCode, long durationMs) {
        LogContext context = LogContext.create("EXTERNAL_SERVICE_CALL", userId)
            .withMetadata("service", service)
            .withMetadata("endpoint", endpoint)
            .withMetadata("status_code", statusCode);
        logPerformance("EXTERNAL_SERVICE_CALL", durationMs, context);
    }

    public void logUserAction(String action, String userId, Map<String, Object> actionData) {
        LogContext context = LogContext.create("USER_ACTION", userId)
            .withMetadata("action", action)
            .withMetadata(actionData);
        logBusiness("USER_ACTION", context, actionData);
    }

    public void logSystemHealth(String component, String status, Map<String, Object> healthData) {
        LogContext context = LogContext.create("SYSTEM_HEALTH")
            .withMetadata("component", component)
            .withMetadata("status", status)
            .withMetadata(healthData);
        logSystem("SYSTEM_HEALTH", context, healthData);
    }

    public void logErrorTracking(String errorType, String userId, Exception exception, Map<String, Object> errorData) {
        LogContext context = LogContext.create("ERROR_TRACKING", userId)
            .withMetadata("error_type", errorType)
            .withMetadata(errorData);
        logError("Error tracked", context, exception, errorData);
    }
}
