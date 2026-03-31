// src/main/java/com/sacmauquan/qrordering/config/SecurityConfig.java
package com.sacmauquan.qrordering.config;

import com.sacmauquan.qrordering.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(c -> c.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

            // Static Admin UI
            .requestMatchers("/admin/**").permitAll()

            // WebSocket & Auth
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/register").permitAll()
            .requestMatchers("/api/auth/**").permitAll()

            // Public GET (keep as is for guests)
            .requestMatchers(HttpMethod.GET,
                "/api/categories/**",
                "/api/menu/**",
                "/api/tables/**",
                "/api/combos/**",
                "/api/orders/table/*/current",
                "/api/vouchers/**",
                "/api/combos/active",
                "/api/combos/*/items",
                "/api/recommendations/**")
            .permitAll()
            // Guest order creation
            .requestMatchers(HttpMethod.POST, "/api/orders/**").permitAll()

            // admin
            .requestMatchers(HttpMethod.POST,
                "/api/categories/**",
                "/api/menu/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/combos/*/items",
                "/api/admin/ai/**")
            .hasAnyRole("MANAGER", "STAFF")

            .requestMatchers(HttpMethod.PUT,
                "/api/categories/**",
                "/api/menu/**",
                "/api/vouchers/**")
            .hasRole("MANAGER")

            // Kitchen / KDS (Must be placed before /api/orders/** to prioritize specific routes)
            .requestMatchers(HttpMethod.GET, "/api/orders/history").hasAnyRole("MANAGER", "STAFF")
            .requestMatchers(HttpMethod.GET, "/api/orders/stats").hasAnyRole("MANAGER", "STAFF")
            .requestMatchers(HttpMethod.GET, "/api/orders/kitchen").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.PATCH, "/api/orders/items/*/status").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.PATCH, "/api/orders/items/*/prepared").hasAnyRole("MANAGER", "STAFF", "CHEF")

            .requestMatchers(HttpMethod.PATCH,
                "/api/categories/**",
                "/api/menu/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/users/*/reset-password",
                "/api/users/**",
                "/api/orders/**")
            .hasAnyRole("MANAGER")

            .requestMatchers(HttpMethod.DELETE,
                "/api/categories/**",
                "/api/menu/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/combos/*/items")
            .hasRole("MANAGER")
            .requestMatchers(HttpMethod.PATCH, "/api/combos/**").hasRole("MANAGER")

            // revenue / stats
            .requestMatchers(HttpMethod.GET, "/api/stats/**").hasAnyRole("MANAGER", "STAFF")

            // upload avatar
            .requestMatchers(HttpMethod.POST, "/api/users/*/avatar").permitAll()

            // Allow /error to avoid error loops
            .requestMatchers("/error").permitAll()

            // Others require authentication
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var c = new CorsConfiguration();
    c.addAllowedOriginPattern("*");
    c.setAllowedOrigins(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "https://order-by-qr.vercel.app", // Your Vercel domain
        "https://order-by-qr.onrender.com" // Backend domain on Render
    ));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
