package com.example.marketsupplier.agent.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.marketsupplier.agent.metrics.AgentMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private boolean isRedisAvailable = false;

    @Value("${agent.state.require_redis:false}")
    private boolean requireRedis;

    @Value("${agent.state.local_fallback_ttl_minutes:10}")
    private int localFallbackTtlMinutes;

    @Value("${agent.state.redis_ttl_minutes:120}")
    private int redisTtlMinutes;

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final AgentMetrics metrics;

    public ContextManager(AgentMetrics metrics) {
        this.metrics = metrics;
    }

    @PostConstruct
    public void validate() {
        if (requireRedis && redisTemplate == null) {
            throw new IllegalStateException("Redis required but not configured. Set agent.state.require_redis=false for single-instance mode.");
        }
        if (redisTemplate != null) {
            try {
                // Check Redis connection
                redisTemplate.execute((RedisConnection connection) -> connection.ping());
                isRedisAvailable = true;
                log.info("Successfully connected to Redis.");
            } catch (Exception e) {
                log.warn("Could not connect to Redis. Falling back to local cache. Error: {}", e.getMessage());
                isRedisAvailable = false;
                if (requireRedis) {
                    throw new IllegalStateException("Redis is required but could not connect.", e);
                }
            }
        } else {
            log.warn("Redis template not configured. Using local cache.");
            isRedisAvailable = false;
        }

        if (localFallbackTtlMinutes < 1) localFallbackTtlMinutes = 10;
        if (redisTtlMinutes < 1) redisTtlMinutes = 120;
    }

    // Sadece cartId ve metadata tutacak şekilde güncellendi
    public CartContext getContext(String phone) {
        String key = buildKey(phone);
        if (isRedisAvailable) {
            try {
                Object obj = redisTemplate.opsForValue().get(key);
                if (obj instanceof CartContext) {
                    return (CartContext) obj;
                }
            } catch (Exception e) {
                log.warn("Redis get failed for {}. Falling back to local cache.", key, e);
                try { if (metrics != null) metrics.incRedisFallback("get_error"); } catch (Exception ignored) {}
            }
        }

        CacheEntry ce = localCache.get(key);
        if (ce == null) return null;
        if (ce.expiresAtMs < System.currentTimeMillis()) {
            localCache.remove(key);
            return null;
        }
        return ce.cartContext;
    }

    public void saveContext(String phone, CartContext cartContext) {
        String key = buildKey(phone);
        if (isRedisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, cartContext, Duration.ofMinutes(redisTtlMinutes));
                localCache.remove(key);
                return;
            } catch (Exception e) {
                log.warn("Redis save failed for {}. Caching locally for short TTL.", key, e);
                try { if (metrics != null) metrics.incRedisFallback("save_error"); } catch (Exception ignored) {}
            }
        }

        long ttlMs = Duration.ofMinutes(localFallbackTtlMinutes).toMillis();
        localCache.put(key, new CacheEntry(cartContext, System.currentTimeMillis() + ttlMs));
    }

    private String buildKey(String phone) {
        return "cart_context:" + phone;
    }
    
    public void clearLocalCache(String phone) {
        String key = buildKey(phone);
        localCache.remove(key);
        log.debug("Cleared local cache for key: {}", key);
    }

    public boolean isRedisAvailable() {
        return isRedisAvailable;
    }

    // Sadece cartId ve metadata tutan basit context
    public static class CartContext implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long cartId;
        private String phone;
        private Long marketId;
        private Map<String, Object> metadata = new ConcurrentHashMap<>();

        public CartContext() {}

        public CartContext(String phone, Long marketId) {
            this.phone = phone;
            this.marketId = marketId;
        }

        // Getters/setters
        public Long getCartId() { return cartId; }
        public void setCartId(Long cartId) { this.cartId = cartId; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Long getMarketId() { return marketId; }
        public void setMarketId(Long marketId) { this.marketId = marketId; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    private static class CacheEntry {
        final CartContext cartContext;
        final long expiresAtMs;
        CacheEntry(CartContext cartContext, long expiresAtMs) {
            this.cartContext = cartContext;
            this.expiresAtMs = expiresAtMs;
        }
    }
}