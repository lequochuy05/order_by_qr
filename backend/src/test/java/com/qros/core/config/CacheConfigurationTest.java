package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.qros.modules.menu.dto.publicmenu.PublicCatalogResponse;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.shared.config.AppProperties;
import com.qros.shared.security.CachedUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

class CacheConfigurationTest {

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

    @Test
    void redisObjectMapperAllowsApplicationDtosAndRejectsUntrustedGadgetTypes() {
        GenericJacksonJsonRedisSerializer serializer = RedisConfig.createRedisSerializer();

        List<PublicCategoryItem> dtoList = List.of(new PublicCategoryItem(1L, "Khai vị", "img.png", 1));
        byte[] bytes = serializer.serialize(dtoList);
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isEqualTo(dtoList);

        PublicCatalogResponse catalog = new PublicCatalogResponse(dtoList, List.of(), List.of());
        assertThat(serializer.deserialize(serializer.serialize(catalog))).isEqualTo(catalog);

        CachedUserPrincipal principal = new CachedUserPrincipal(
                7L, "manager@qros.vn", "MANAGER", true, true, LocalDateTime.of(2026, 11, 24, 12, 0), false);
        byte[] principalBytes = serializer.serialize(principal);
        assertThat(new String(principalBytes)).doesNotContain("password\"");
        Object deserializedPrincipal = serializer.deserialize(principalBytes);
        assertThat(deserializedPrincipal).isEqualTo(principal);
        assertThat(((CachedUserPrincipal) deserializedPrincipal).getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MANAGER");

        assertThat(serializer.deserialize(serializer.serialize(Map.of()))).isEqualTo(Map.of());

        byte[] propertyGadgetPayload = "{\"@class\":\"java.lang.ProcessBuilder\",\"command\":[\"id\"]}".getBytes();
        assertThatThrownBy(() -> serializer.deserialize(propertyGadgetPayload))
                .isInstanceOf(SerializationException.class);

        byte[] wrapperGadgetPayload = "[\"java.lang.ProcessBuilder\",{\"command\":[\"id\"]}]".getBytes();
        assertThatThrownBy(() -> serializer.deserialize(wrapperGadgetPayload))
                .isInstanceOf(SerializationException.class);
    }
}
