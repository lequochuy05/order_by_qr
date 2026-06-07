package com.qros.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisConfig - Configuration for Redis caching and template.
 * Enables caching and sets up RedisTemplate with JSON serialization.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis Cache GET failed for key {}: {}", key, exception.getMessage());
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
        // NON_FINAL is safer than EVERYTHING: it still serializes type info for
        // polymorphic deserialization but limits it to non-final classes, avoiding
        // deserialization attack vectors through final/critical JDK types.
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
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
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("categories", config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("menu", config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("combos", config.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("qrcodes", config.entryTtl(Duration.ofDays(7)));
        cacheConfigurations.put("tables", config.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("vouchers", config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("recommendations", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("popularItems", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("stats_revenue", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("stats_emp_performance", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("stats_top_dishes", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("stats_dish_trend", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("settings", config.entryTtl(Duration.ofHours(1)));

        // New performance caches
        cacheConfigurations.put("order_by_id", config.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("order_stats", config.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("stats_dashboard", config.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}
