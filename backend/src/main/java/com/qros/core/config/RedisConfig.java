package com.qros.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.qros.shared.cache.CacheNames;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

/**
 * RedisConfig - Configuration for Redis caching and template.
 * Enables caching and sets up RedisTemplate with JSON serialization.
 * Activated by spring.cache.type=redis. Dev uses Caffeine by default.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "Redis Cache GET failed for cache={}, key={}: {}. Falling back to source data.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception.getMessage());
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

    static GenericJacksonJsonRedisSerializer createRedisSerializer() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.qros.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .allowIfSubType("org.springframework.data.domain.")
                .allowIfSubType(Record.class)
                .allowIfSubType(Enum.class)
                .allowIfSubType(Number.class)
                .allowIfSubType(String.class)
                .allowIfSubType(Boolean.class)
                .allowIfSubTypeIsArray()
                .build();

        DefaultTypeResolverBuilder typeResolver =
                new DefaultTypeResolverBuilder(typeValidator, DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY) {
                    @Override
                    public boolean useForType(JavaType t) {
                        if (t.isPrimitive() || t.isEnumType() || TreeNode.class.isAssignableFrom(t.getRawClass())) {
                            return false;
                        }
                        Class<?> raw = t.getRawClass();
                        if (raw == String.class || Number.class.isAssignableFrom(raw) || raw == Boolean.class) {
                            return false;
                        }
                        return true;
                    }
                };

        JsonMapper mapper = JsonMapper.builder().setDefaultTyping(typeResolver).build();
        return new GenericJacksonJsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJacksonJsonRedisSerializer serializer = createRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, AppProperties appProperties) {
        GenericJacksonJsonRedisSerializer serializer = createRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith(appProperties.getCache().getPrefix())
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
