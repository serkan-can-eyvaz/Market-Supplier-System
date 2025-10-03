package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.CircuitBreakerService;
import com.example.marketsupplier.service.CriticalServiceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resilience")
public class ResilienceController {

    @Autowired
    private CriticalServiceWrapper criticalServiceWrapper;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @GetMapping("/health")
    public ResponseEntity<CriticalServiceWrapper.ServiceHealthStatus> getServiceHealth() {
        CriticalServiceWrapper.ServiceHealthStatus health = criticalServiceWrapper.getServiceHealthStatus();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStats>> getCircuitBreakerStats() {
        Map<String, CircuitBreakerService.CircuitBreakerStats> stats = criticalServiceWrapper.getAllCircuitStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/circuit-breakers/{serviceName}")
    public ResponseEntity<CircuitBreakerService.CircuitBreakerStats> getCircuitBreakerStats(@PathVariable String serviceName) {
        CircuitBreakerService.CircuitBreakerStats stats = criticalServiceWrapper.getCircuitStats(serviceName);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/circuit-breakers/reset")
    public ResponseEntity<String> resetAllCircuitBreakers() {
        criticalServiceWrapper.resetAllCircuits();
        return ResponseEntity.ok("All circuit breakers have been reset");
    }

    @PostMapping("/circuit-breakers/{serviceName}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String serviceName) {
        circuitBreakerService.resetCircuit(serviceName);
        return ResponseEntity.ok("Circuit breaker for " + serviceName + " has been reset");
    }

    @PostMapping("/circuit-breakers/{serviceName}/force-open")
    public ResponseEntity<String> forceOpenCircuitBreaker(@PathVariable String serviceName) {
        criticalServiceWrapper.forceOpenCircuit(serviceName);
        return ResponseEntity.ok("Circuit breaker for " + serviceName + " has been forced open");
    }

    @PostMapping("/circuit-breakers/{serviceName}/force-close")
    public ResponseEntity<String> forceCloseCircuitBreaker(@PathVariable String serviceName) {
        criticalServiceWrapper.forceCloseCircuit(serviceName);
        return ResponseEntity.ok("Circuit breaker for " + serviceName + " has been forced closed");
    }

    @GetMapping("/services/status")
    public ResponseEntity<Map<String, Object>> getServicesStatus() {
        Map<String, Object> status = Map.of(
            "llm", criticalServiceWrapper.isLlmServiceHealthy(),
            "redis", criticalServiceWrapper.isRedisServiceHealthy(),
            "whatsapp", criticalServiceWrapper.isWhatsAppServiceHealthy(),
            "database", criticalServiceWrapper.isDatabaseServiceHealthy(),
            "cache", criticalServiceWrapper.isCacheServiceHealthy()
        );
        return ResponseEntity.ok(status);
    }
}
