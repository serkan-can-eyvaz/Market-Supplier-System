package com.example.marketsupplier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.default-ttl:300}")
    private long defaultTtlSeconds;

    @Value("${app.cache.local-fallback:true}")
    private boolean localFallback;

    // Local cache as fallback
    private final java.util.concurrent.ConcurrentHashMap<String, CacheEntry> localCache = 
        new java.util.concurrent.ConcurrentHashMap<>();

    public <T> Optional<T> get(String key, Class<T> clazz) {
        if (!cacheEnabled) {
            return Optional.empty();
        }

        try {
            // Try Redis first
            if (redisTemplate != null) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    log.debug("Cache hit (Redis) for key: {}", key);
                    return Optional.of(objectMapper.readValue(value, clazz));
                }
            }

            // Fallback to local cache
            if (localFallback) {
                CacheEntry entry = localCache.get(key);
                if (entry != null && !entry.isExpired()) {
                    log.debug("Cache hit (Local) for key: {}", key);
                    return Optional.of(objectMapper.readValue(entry.getValue(), clazz));
                }
            }

            log.debug("Cache miss for key: {}", key);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Error retrieving from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    public <T> void put(String key, T value) {
        put(key, value, defaultTtlSeconds);
    }

    public <T> void put(String key, T value, long ttlSeconds) {
        if (!cacheEnabled) {
            return;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(value);

            // Try Redis first
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, jsonValue, Duration.ofSeconds(ttlSeconds));
                log.debug("Cached (Redis) key: {} with TTL: {}s", key, ttlSeconds);
            }

            // Always update local cache as fallback
            if (localFallback) {
                localCache.put(key, new CacheEntry(jsonValue, System.currentTimeMillis() + (ttlSeconds * 1000)));
                log.debug("Cached (Local) key: {} with TTL: {}s", key, ttlSeconds);
            }

        } catch (Exception e) {
            log.warn("Error caching value for key: {}", key, e);
        }
    }

    public void evict(String key) {
        if (!cacheEnabled) {
            return;
        }

        try {
            // Remove from Redis
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }

            // Remove from local cache
            if (localFallback) {
                localCache.remove(key);
            }

            log.debug("Evicted cache key: {}", key);

        } catch (Exception e) {
            log.warn("Error evicting cache key: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        if (!cacheEnabled) {
            return;
        }

        try {
            // Remove from Redis
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
                }
            }

            // Remove from local cache
            if (localFallback) {
                localCache.entrySet().removeIf(entry -> entry.getKey().matches(pattern));
                log.debug("Evicted local cache keys matching pattern: {}", pattern);
            }

        } catch (Exception e) {
            log.warn("Error evicting cache pattern: {}", pattern, e);
        }
    }

    public boolean exists(String key) {
        if (!cacheEnabled) {
            return false;
        }

        try {
            // Check Redis first
            if (redisTemplate != null) {
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            }

            // Check local cache
            if (localFallback) {
                CacheEntry entry = localCache.get(key);
                return entry != null && !entry.isExpired();
            }

            return false;

        } catch (Exception e) {
            log.warn("Error checking cache existence for key: {}", key, e);
            return false;
        }
    }

    public long getTtl(String key) {
        if (!cacheEnabled) {
            return -1;
        }

        try {
            // Check Redis first
            if (redisTemplate != null) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl > 0) {
                    return ttl;
                }
            }

            // Check local cache
            if (localFallback) {
                CacheEntry entry = localCache.get(key);
                if (entry != null && !entry.isExpired()) {
                    return (entry.getExpiryTime() - System.currentTimeMillis()) / 1000;
                }
            }

            return -1;

        } catch (Exception e) {
            log.warn("Error getting TTL for key: {}", key, e);
            return -1;
        }
    }

    public void clear() {
        if (!cacheEnabled) {
            return;
        }

        try {
            // Clear Redis
            if (redisTemplate != null) {
                redisTemplate.getConnectionFactory().getConnection().flushAll();
            }

            // Clear local cache
            if (localFallback) {
                localCache.clear();
            }

            log.info("Cleared all cache");

        } catch (Exception e) {
            log.warn("Error clearing cache", e);
        }
    }

    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        
        try {
            // Redis stats
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys("*");
                stats.setRedisKeys(keys != null ? keys.size() : 0);
            }

            // Local cache stats
            if (localFallback) {
                stats.setLocalKeys(localCache.size());
                stats.setExpiredKeys((int) localCache.values().stream()
                    .filter(CacheEntry::isExpired)
                    .count());
            }

        } catch (Exception e) {
            log.warn("Error getting cache stats", e);
        }

        return stats;
    }

    // Cache entry for local fallback
    private static class CacheEntry {
        private final String value;
        private final long expiryTime;

        public CacheEntry(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public String getValue() {
            return value;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // Cache statistics
    public static class CacheStats {
        private int redisKeys = 0;
        private int localKeys = 0;
        private int expiredKeys = 0;

        public int getRedisKeys() { return redisKeys; }
        public void setRedisKeys(int redisKeys) { this.redisKeys = redisKeys; }
        public int getLocalKeys() { return localKeys; }
        public void setLocalKeys(int localKeys) { this.localKeys = localKeys; }
        public int getExpiredKeys() { return expiredKeys; }
        public void setExpiredKeys(int expiredKeys) { this.expiredKeys = expiredKeys; }

        @Override
        public String toString() {
            return String.format("CacheStats{redisKeys=%d, localKeys=%d, expiredKeys=%d}", 
                redisKeys, localKeys, expiredKeys);
        }
    }
}
