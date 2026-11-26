package com.qros.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.qros.shared.cache.CacheNames;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats());
        manager.registerCustomCache(
                CacheNames.USER_AUTH,
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(2_000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(
                CacheNames.MENU_AVAILABILITY,
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(2_000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(
                CacheNames.ORDER_ID_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(5_000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(
                CacheNames.ORDER_STATS,
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(
                CacheNames.AI_MENU_CONTEXT,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());
        return manager;
    }
}
