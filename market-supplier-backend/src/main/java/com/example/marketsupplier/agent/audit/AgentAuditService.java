package com.example.marketsupplier.agent.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditService.class);

    public void record(String conversationId, String phone, String event, String payloadJson) {
        // Production: persist to audit_logs table or endpoint
        log.info("AUDIT conversation={} phone={} event={} payload={}", conversationId, phone, event, payloadJson);
    }
}
