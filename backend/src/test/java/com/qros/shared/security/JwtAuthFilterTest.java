package com.qros.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private TokenBlacklistService tokenBlacklistService;
    private ObjectMapper objectMapper;
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService, tokenBlacklistService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalWhenNoAuthHeaderPassesThrough() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalWhenUserDoesNotNeedPasswordChangeAllowsAccess() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenType("valid-token")).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.extractSubject("valid-token")).thenReturn("staff@qros.vn");

        User user = new User();
        user.setId(2L);
        user.setEmail("staff@qros.vn");
        user.setRole(UserRole.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);
        when(userDetailsService.loadUserByUsername("staff@qros.vn")).thenReturn(user);

        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternalWhenMustChangePasswordBlocksBusinessEndpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenType("valid-token")).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.extractSubject("valid-token")).thenReturn("staff@qros.vn");

        User user = new User();
        user.setId(2L);
        user.setEmail("staff@qros.vn");
        user.setRole(UserRole.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        when(userDetailsService.loadUserByUsername("staff@qros.vn")).thenReturn(user);

        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(403);
        assertThat(responseBody.toString()).contains("PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    void doFilterInternalWhenMustChangePasswordAllowsPasswordUpdateEndpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenType("valid-token")).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.extractSubject("valid-token")).thenReturn("staff@qros.vn");

        User user = new User();
        user.setId(2L);
        user.setEmail("staff@qros.vn");
        user.setRole(UserRole.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        when(userDetailsService.loadUserByUsername("staff@qros.vn")).thenReturn(user);

        when(request.getRequestURI()).thenReturn("/api/v1/users/me/password");
        when(request.getMethod()).thenReturn("PATCH");

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternalWhenMustChangePasswordAllowsProfileGetEndpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenType("valid-token")).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.extractSubject("valid-token")).thenReturn("staff@qros.vn");

        User user = new User();
        user.setId(2L);
        user.setEmail("staff@qros.vn");
        user.setRole(UserRole.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        when(userDetailsService.loadUserByUsername("staff@qros.vn")).thenReturn(user);

        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternalWhenMustChangePasswordBlocksProfilePatchEndpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenType("valid-token")).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtService.extractSubject("valid-token")).thenReturn("staff@qros.vn");

        User user = new User();
        user.setId(2L);
        user.setEmail("staff@qros.vn");
        user.setRole(UserRole.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        when(userDetailsService.loadUserByUsername("staff@qros.vn")).thenReturn(user);

        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getMethod()).thenReturn("PATCH");

        jwtAuthFilter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(403);
        assertThat(responseBody.toString()).contains("PASSWORD_CHANGE_REQUIRED");
    }
}
