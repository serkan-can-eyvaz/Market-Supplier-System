package com.example.marketsupplier.agent.dedup;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DedupService {

    private static class Entry { long ts; }
    private final Map<String, Entry> seen = new ConcurrentHashMap<>();
    private final long ttlMillis = 5 * 60 * 1000; // 5 minutes

    public boolean isDuplicate(String messageId) {
        if (messageId == null) return false;
        long now = Instant.now().toEpochMilli();
        Entry e = seen.get(messageId);
        if (e != null && (now - e.ts) < ttlMillis) return true;
        Entry n = new Entry(); n.ts = now; seen.put(messageId, n);
        // Cleanup opportunistically
        seen.entrySet().removeIf(kv -> (now - kv.getValue().ts) > ttlMillis);
        return false;
    }
}
