package com.example.marketsupplier.agent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AgentMetrics {

    private final MeterRegistry registry;

    public AgentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incIntent(String intent) {
        Counter.builder("agent.intent")
                .tag("name", intent == null ? "unknown" : intent)
                .register(registry)
                .increment();
    }

    public void incReserve(boolean success) {
        Counter.builder("agent.reserve")
                .tag("status", success ? "success" : "fail")
                .register(registry)
                .increment();
    }

    public void incEscalation() {
        Counter.builder("agent.escalation.count")
                .register(registry)
                .increment();
    }

    public void incAction(String action) {
        Counter.builder("agent.action")
                .tag("name", action == null ? "unknown" : action)
                .register(registry)
                .increment();
    }

    public void incAwaitingConfirm(String event) { // event: set|clear
        Counter.builder("agent.confirm.awaiting")
                .tag("event", event)
                .register(registry)
                .increment();
    }

    public void incDbFallback(String operation) {
        Counter.builder("agent.db.fallback")
                .tag("op", operation)
                .register(registry)
                .increment();
    }

    public void incLlmFallback(String type) {
        Counter.builder("agent.llm.fallback")
                .tag("type", type)
                .register(registry)
                .increment();
    }

    public void incRedisFallback(String type) {
        Counter.builder("agent.redis.fallback")
                .tag("type", type)
                .register(registry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample, String name, String... tags) {
        Timer.Builder builder = Timer.builder(name);
        if (tags != null && tags.length % 2 == 0) {
            builder.tags(tags);
        }
        sample.stop(builder.register(registry));
    }
}
