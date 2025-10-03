package com.example.marketsupplier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.serialization.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.redis.enabled:false}")
    private boolean redisCacheEnabled;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if (cacheEnabled && redisCacheEnabled) {
            // Redis cache manager
            RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
                    .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofMinutes(30)));

            // Configure specific cache TTLs
            Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("products", 
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(60)));
            
            cacheConfigurations.put("markets", 
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)));
            
            cacheConfigurations.put("orders", 
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(15)));

            builder.withInitialCacheConfigurations(cacheConfigurations);
            return builder.build();
        } else {
            // Local cache manager as fallback
            return new ConcurrentMapCacheManager("products", "markets", "orders", "users");
        }
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
