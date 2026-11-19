package com.qros.core.config.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qros.core.config.AppProperties;
import com.qros.modules.settings.controller.PublicSettingsController;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.user.controller.CurrentUserController;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.modules.user.service.UserAvatarService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.rate_limit.RateLimitProperties;
import com.qros.shared.rate_limit.RedisRateLimiter;
import com.qros.shared.security.JwtAuthFilter;
import com.qros.shared.security.JwtService;
import com.qros.shared.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PublicSettingsController.class, CurrentUserController.class})
@ContextConfiguration(classes = CoreApiSecurityIntegrationTest.TestApplication.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    JwtAuthFilter.class,
    PublicSettingsController.class,
    CurrentUserController.class,
    CoreApiSecurityIntegrationTest.PropertiesConfiguration.class
})
class CoreApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemSettingsService settingsService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserAvatarService userAvatarService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private RedisRateLimiter redisRateLimiter;

    @MockitoBean
    private ClientAddressResolver clientAddressResolver;

    @Test
    void servesPublicSettingsThroughTheConfiguredSecurityChain() throws Exception {
        mockMvc.perform(get(ApiRoutes.PUBLIC_SETTINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200));

        verify(settingsService).getPublicSettings();
    }

    @Test
    void rejectsAnonymousRequestsToTheCurrentUserEndpoint() throws Exception {
        mockMvc.perform(get(ApiRoutes.USERS + "/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(currentUserService);
    }

    @Test
    void permitsAuthenticatedRequestsAndPassesPrincipalToTheService() throws Exception {
        mockMvc.perform(get(ApiRoutes.USERS + "/me")
                        .with(user("staff@example.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200));

        verify(currentUserService).getCurrentProfile("staff@example.test");
    }

    @TestConfiguration
    @EnableConfigurationProperties({AppProperties.class, RateLimitProperties.class})
    static class PropertiesConfiguration {}

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
