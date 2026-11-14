package com.qros.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JwtAuthFilter - Security filter that intercepts each HTTP request to validate
 * JWT tokens.
 * Extracts the token from the 'Authorization' header and establishes security
 * context.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
            JwtService jwtService, UserDetailsService userDetailsService, TokenBlacklistService tokenBlacklistService) {
        this(jwtService, userDetailsService, tokenBlacklistService, new ObjectMapper());
    }

    @Autowired
    public JwtAuthFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            TokenBlacklistService tokenBlacklistService,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Performs the filtering logic: validates the Bearer token and sets
     * authentication in SecurityContext.
     *
     * @param request  The incoming HTTP request
     * @param response The outgoing HTTP response
     * @param chain    The filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isValid(token)
                && "access".equals(jwtService.extractTokenType(token))) {

            if (tokenBlacklistService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            try {
                String email = jwtService.extractSubject(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (isUsableAccount(userDetails) && isFreshForPasswordVersion(token, userDetails)) {
                    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    if (isPasswordChangeRequired(userDetails, request)) {
                        writePasswordChangeRequiredResponse(response);
                        return;
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isPasswordChangeRequired(UserDetails userDetails, HttpServletRequest request) {
        if (!(userDetails instanceof PasswordVersionedUserDetails pwdUser) || !pwdUser.isMustChangePassword()) {
            return false;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        if (path != null && path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        String servletPath = request.getServletPath();
        if (servletPath != null && servletPath.endsWith("/") && servletPath.length() > 1) {
            servletPath = servletPath.substring(0, servletPath.length() - 1);
        }

        String method = request.getMethod();
        boolean isMeProfile = HttpMethod.GET.matches(method)
                && ((ApiRoutes.USERS + "/me").equals(path) || (ApiRoutes.USERS + "/me").equals(servletPath));
        boolean isChangePassword = HttpMethod.PATCH.matches(method)
                && ((ApiRoutes.USERS + "/me/password").equals(path)
                        || (ApiRoutes.USERS + "/me/password").equals(servletPath));
        boolean isLogout = HttpMethod.POST.matches(method)
                && ((ApiRoutes.AUTH + "/logout").equals(path) || (ApiRoutes.AUTH + "/logout").equals(servletPath));

        return !(isMeProfile || isChangePassword || isLogout);
    }

    private void writePasswordChangeRequiredResponse(HttpServletResponse response) throws IOException {
        int status = HttpStatus.FORBIDDEN.value();
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.PASSWORD_CHANGE_REQUIRED.name())
                .message(ErrorCode.PASSWORD_CHANGE_REQUIRED.getDefaultMessage())
                .details(Map.of())
                .build();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(status, ErrorCode.PASSWORD_CHANGE_REQUIRED.getDefaultMessage(), error));
    }

    private boolean isUsableAccount(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }

    private boolean isFreshForPasswordVersion(String token, UserDetails userDetails) {
        if (!(userDetails instanceof PasswordVersionedUserDetails passwordVersionedUser)) {
            return true;
        }

        LocalDateTime passwordChangedAt = passwordVersionedUser.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return true;
        }

        Date issuedAt = jwtService.extractIssuedAt(token);
        if (issuedAt == null) {
            return false;
        }

        long issuedAtSec = issuedAt.getTime() / 1000;
        long passwordChangedAtSec =
                passwordChangedAt.atZone(com.qros.shared.time.AppTime.ZONE).toEpochSecond();
        return issuedAtSec >= passwordChangedAtSec;
    }
}
