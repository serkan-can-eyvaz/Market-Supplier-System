package com.example.marketsupplier.security;

import com.example.marketsupplier.util.LoggerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuditLogService {

    @Autowired
    private LoggerUtility loggerUtility;

    private final Map<String, AuditLogEntry> auditLogs = new ConcurrentHashMap<>();

    public void logAuthentication(String username, String action, String result, String ipAddress, String userAgent) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_AUTH")
            .withMetadata("username", username)
            .withMetadata("action", action)
            .withMetadata("result", result)
            .withMetadata("ip_address", ipAddress);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("AUTHENTICATION")
                .username(username)
                .action(action)
                .result(result)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(Map.of("action", action, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("Authentication audit logged", context, Map.of("operation", "auth_audit"));
    }

    public void logAuthorization(String username, String resource, String action, String result, String reason) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_AUTHZ")
            .withMetadata("username", username)
            .withMetadata("resource", resource)
            .withMetadata("action", action)
            .withMetadata("result", result);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("AUTHORIZATION")
                .username(username)
                .action(action)
                .result(result)
                .resource(resource)
                .reason(reason)
                .details(Map.of("resource", resource, "action", action, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("Authorization audit logged", context, Map.of("operation", "authz_audit"));
    }

    public void logDataAccess(String username, String dataType, String action, String recordId, String result) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_DATA_ACCESS")
            .withMetadata("username", username)
            .withMetadata("data_type", dataType)
            .withMetadata("action", action)
            .withMetadata("record_id", recordId)
            .withMetadata("result", result);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("DATA_ACCESS")
                .username(username)
                .action(action)
                .result(result)
                .resource(dataType)
                .recordId(recordId)
                .details(Map.of("dataType", dataType, "action", action, "recordId", recordId, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("Data access audit logged", context, Map.of("operation", "data_access_audit"));
    }

    public void logDataModification(String username, String dataType, String action, String recordId, String oldValue, String newValue) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_DATA_MODIFICATION")
            .withMetadata("username", username)
            .withMetadata("data_type", dataType)
            .withMetadata("action", action)
            .withMetadata("record_id", recordId);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("DATA_MODIFICATION")
                .username(username)
                .action(action)
                .result("SUCCESS")
                .resource(dataType)
                .recordId(recordId)
                .details(Map.of(
                    "dataType", dataType,
                    "action", action,
                    "recordId", recordId,
                    "oldValue", oldValue,
                    "newValue", newValue
                ))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("Data modification audit logged", context, Map.of("operation", "data_modification_audit"));
    }

    public void logSecurityEvent(String eventType, String description, String severity, String username, String ipAddress) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_SECURITY")
            .withMetadata("event_type", eventType)
            .withMetadata("severity", severity)
            .withMetadata("username", username)
            .withMetadata("ip_address", ipAddress);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("SECURITY_EVENT")
                .username(username)
                .action(eventType)
                .result(severity)
                .ipAddress(ipAddress)
                .details(Map.of(
                    "eventType", eventType,
                    "description", description,
                    "severity", severity
                ))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("Security event audit logged", context, Map.of("operation", "security_event_audit"));
    }

    public void logOAuth2TokenRequest(String service, String result, LoggerUtility.LogContext context) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("OAUTH2_TOKEN_REQUEST")
                .username("system")
                .action("TOKEN_REQUEST")
                .result(result)
                .resource(service)
                .details(Map.of("service", service, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("OAuth2 token request audit logged", context, Map.of("operation", "oauth2_token_request"));
    }

    public void logOAuth2TokenRefresh(String service, String result, LoggerUtility.LogContext context) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("OAUTH2_TOKEN_REFRESH")
                .username("system")
                .action("TOKEN_REFRESH")
                .result(result)
                .resource(service)
                .details(Map.of("service", service, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("OAuth2 token refresh audit logged", context, Map.of("operation", "oauth2_token_refresh"));
    }

    public void logOAuth2TokenValidation(String service, String result, LoggerUtility.LogContext context) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("OAUTH2_TOKEN_VALIDATION")
                .username("system")
                .action("TOKEN_VALIDATION")
                .result(result)
                .resource(service)
                .details(Map.of("service", service, "result", result))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("OAuth2 token validation audit logged", context, Map.of("operation", "oauth2_token_validation"));
    }

    public void logPIIAccess(String username, String dataType, String action, String recordId, String result) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_PII_ACCESS")
            .withMetadata("username", username)
            .withMetadata("data_type", dataType)
            .withMetadata("action", action)
            .withMetadata("record_id", recordId)
            .withMetadata("result", result);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("PII_ACCESS")
                .username(username)
                .action(action)
                .result(result)
                .resource(dataType)
                .recordId(recordId)
                .details(Map.of(
                    "dataType", dataType,
                    "action", action,
                    "recordId", recordId,
                    "result", result,
                    "piiAccess", true
                ))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("PII access audit logged", context, Map.of("operation", "pii_access_audit"));
    }

    public void logGDPRRequest(String username, String requestType, String result, String details) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_GDPR")
            .withMetadata("username", username)
            .withMetadata("request_type", requestType)
            .withMetadata("result", result);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("GDPR_REQUEST")
                .username(username)
                .action(requestType)
                .result(result)
                .details(Map.of(
                    "requestType", requestType,
                    "result", result,
                    "details", details,
                    "gdprRequest", true
                ))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSecurity("GDPR request audit logged", context, Map.of("operation", "gdpr_request_audit"));
    }

    public void logSystemEvent(String eventType, String description, String result) {
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("AUDIT_SYSTEM")
            .withMetadata("event_type", eventType)
            .withMetadata("result", result);

        AuditLogEntry entry = AuditLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .eventType("SYSTEM_EVENT")
                .username("system")
                .action(eventType)
                .result(result)
                .details(Map.of(
                    "eventType", eventType,
                    "description", description,
                    "result", result
                ))
                .build();

        storeAuditLog(entry);
        loggerUtility.logSystem("System event audit logged", context, Map.of("operation", "system_event_audit"));
    }

    private void storeAuditLog(AuditLogEntry entry) {
        String logId = generateLogId(entry);
        auditLogs.put(logId, entry);
        
        // In production, this would be stored in a secure database
        // For now, we're using in-memory storage
    }

    private String generateLogId(AuditLogEntry entry) {
        return entry.getEventType() + "_" + entry.getTimestamp().toString() + "_" + 
               (entry.getUsername() != null ? entry.getUsername() : "system");
    }

    public Map<String, AuditLogEntry> getAuditLogs() {
        return new HashMap<>(auditLogs);
    }

    public Map<String, AuditLogEntry> getAuditLogsByUser(String username) {
        return auditLogs.entrySet().stream()
                .filter(entry -> username.equals(entry.getValue().getUsername()))
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }

    public Map<String, AuditLogEntry> getAuditLogsByEventType(String eventType) {
        return auditLogs.entrySet().stream()
                .filter(entry -> eventType.equals(entry.getValue().getEventType()))
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }

    public Map<String, AuditLogEntry> getAuditLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogs.entrySet().stream()
                .filter(entry -> {
                    LocalDateTime timestamp = entry.getValue().getTimestamp();
                    return timestamp.isAfter(start) && timestamp.isBefore(end);
                })
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }

    public static class AuditLogEntry {
        private LocalDateTime timestamp;
        private String eventType;
        private String username;
        private String action;
        private String result;
        private String resource;
        private String recordId;
        private String ipAddress;
        private String userAgent;
        private String reason;
        private Map<String, Object> details;

        private AuditLogEntry() {}

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AuditLogEntry entry = new AuditLogEntry();

            public Builder timestamp(LocalDateTime timestamp) {
                entry.timestamp = timestamp;
                return this;
            }

            public Builder eventType(String eventType) {
                entry.eventType = eventType;
                return this;
            }

            public Builder username(String username) {
                entry.username = username;
                return this;
            }

            public Builder action(String action) {
                entry.action = action;
                return this;
            }

            public Builder result(String result) {
                entry.result = result;
                return this;
            }

            public Builder resource(String resource) {
                entry.resource = resource;
                return this;
            }

            public Builder recordId(String recordId) {
                entry.recordId = recordId;
                return this;
            }

            public Builder ipAddress(String ipAddress) {
                entry.ipAddress = ipAddress;
                return this;
            }

            public Builder userAgent(String userAgent) {
                entry.userAgent = userAgent;
                return this;
            }

            public Builder reason(String reason) {
                entry.reason = reason;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                entry.details = details;
                return this;
            }

            public AuditLogEntry build() {
                return entry;
            }
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getUsername() { return username; }
        public String getAction() { return action; }
        public String getResult() { return result; }
        public String getResource() { return resource; }
        public String getRecordId() { return recordId; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public String getReason() { return reason; }
        public Map<String, Object> getDetails() { return details; }
    }
}
