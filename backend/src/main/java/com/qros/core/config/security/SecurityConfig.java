package com.qros.core.config.security;

import static com.qros.core.config.security.SecurityRoutes.*;

import com.qros.shared.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                PathRequest.toStaticResources().atCommonLocations())
                        .permitAll()
                        .requestMatchers(STATIC_AND_SYSTEM)
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, AUTH_PUBLIC_POST)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, SELF_GET)
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, SELF_PATCH)
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, SELF_POST)
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, MANAGER_GET)
                        .hasAnyRole("MANAGER", "CHEF")
                        .requestMatchers(HttpMethod.GET, STAFF_READ_GET)
                        .hasAnyRole("MANAGER", "CHEF", "STAFF")
                        .requestMatchers(HttpMethod.GET, OPERATION_GET)
                        .hasAnyRole("MANAGER", "STAFF", "CHEF")
                        .requestMatchers(HttpMethod.GET, KITCHEN_GET)
                        .hasAnyRole("MANAGER", "CHEF")
                        .requestMatchers(HttpMethod.PATCH, KITCHEN_PATCH)
                        .hasAnyRole("MANAGER", "CHEF", "STAFF")
                        .requestMatchers(HttpMethod.POST, STAFF_AI_POST)
                        .hasAnyRole("MANAGER", "STAFF", "CHEF")
                        .requestMatchers(HttpMethod.POST, STAFF_OPERATION_POST)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.PATCH, STAFF_OPERATION_PATCH)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, STAFF_OPERATION_DELETE)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(PAYMENT_ROUTES)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, MANAGER_POST)
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, MANAGER_PUT)
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, MANAGER_PATCH)
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, MANAGER_DELETE)
                        .hasRole("MANAGER")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
