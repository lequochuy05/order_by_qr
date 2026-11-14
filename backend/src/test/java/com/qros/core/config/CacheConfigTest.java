package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

class CacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RedisConfig.class, CaffeineCacheConfig.class)
            .withBean(AppProperties.class, AppProperties::new)
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void redisCacheTypeRegistersRedisCacheManagerOnly() {
        contextRunner.withPropertyValues("spring.cache.type=redis").run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).hasSingleBean(RedisTemplate.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
            assertThat(context).doesNotHaveBean(CaffeineCacheManager.class);
        });
    }

    @Test
    void caffeineCacheTypeRegistersCaffeineCacheManagerOnly() {
        contextRunner.withPropertyValues("spring.cache.type=caffeine").run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
            assertThat(context).doesNotHaveBean(RedisTemplate.class);
        });
    }
}
