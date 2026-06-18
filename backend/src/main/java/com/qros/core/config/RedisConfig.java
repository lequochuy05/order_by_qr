package com.qros.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qros.shared.cache.CacheNames;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig - Configuration for Redis caching and template.
 * Enables caching and sets up RedisTemplate with JSON serialization.
 */
@Configuration
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "Redis Cache GET failed for cache={}, key={}: {}. Evicting the broken entry.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception.getMessage());
                if (cache != null && key != null) {
                    cache.evictIfPresent(key);
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Redis Cache PUT failed for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis Cache EVICT failed for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Redis Cache CLEAR failed: {}", exception.getMessage());
            }
        };
    }

    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // RedisCache deserializes values as Object, so type metadata must also be
        // written for final DTOs/records returned by public APIs.
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper redisObjectMapper = createRedisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = createRedisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("qros:v3:")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheNames.CATEGORIES, config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put(CacheNames.MENU, config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put(CacheNames.PUBLIC_MENU, config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put(CacheNames.COMBOS, config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put(CacheNames.TABLES, config.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.STATS_DASHBOARD, config.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.VOUCHERS, config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(CacheNames.RECOMMENDATIONS, config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.POPULAR_ITEMS, config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.ANALYTICS, config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.SETTINGS, config.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CacheNames.ORDER_ID_CACHE, config.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(CacheNames.ORDER_STATS, config.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(CacheNames.MENU_AVAILABILITY, config.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
