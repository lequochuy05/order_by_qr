// src/main/java/com/sacmauquan/qrordering/core/config/SecurityConfig.java
package com.qros.core.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.qros.shared.security.JwtAuthFilter;

import java.util.List;

/**
 * Configure security settings for the application.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  /**
   * Configures the security filter chain.
   * 
   * @param http HttpSecurity object
   * @return SecurityFilterChain object
   * @throws Exception if an error occurs
   */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(c -> c.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

            // ws
            .requestMatchers("/ws/**", "/error").permitAll()

            // Actuator health + prometheus metrics
            .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

            // auth
            .requestMatchers(HttpMethod.POST,
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/auth/forgot-password-email",
                "/api/auth/reset-password-email",
                "/api/auth/forgot-password-phone",
                "/api/auth/reset-password-phone")
            .permitAll()

            // Customer/public API
            .requestMatchers(HttpMethod.GET, "/api/public/**")
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/api/public/orders")
            .permitAll()

            // Public Voucher Validation only
            .requestMatchers(HttpMethod.GET, "/api/vouchers/validate").permitAll()
            // Guest order creation
            .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/preview").permitAll()
            // AI Customer Assistant (public chat)
            .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()

            // PayOS webhook
            .requestMatchers(HttpMethod.POST, "/api/webhooks/**").permitAll()

            // Authenticated self-service profile endpoints
            .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
            .requestMatchers(HttpMethod.PATCH, "/api/users/me", "/api/users/me/password").authenticated()
            .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").authenticated()

            // Staff administration
            .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/*").hasRole("MANAGER")
            .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/*/avatar").hasRole("MANAGER")
            .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("MANAGER")

            .requestMatchers("/api/payments/**").hasAnyRole("MANAGER", "STAFF")

            .requestMatchers(HttpMethod.GET, "/api/settings").hasRole("MANAGER")
            .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/menu-items/**", "/api/combos/**")
            .hasAnyRole("MANAGER", "STAFF")
            .requestMatchers(HttpMethod.POST, "/api/combos/**").hasRole("MANAGER")

            // admin
            .requestMatchers(HttpMethod.POST,
                "/api/categories/**",
                "/api/menu-items/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/admin/ai/**")
            .hasRole("MANAGER")

            .requestMatchers(HttpMethod.PUT,
                "/api/categories/**",
                "/api/menu-items/**",
                "/api/combos/**",
                "/api/vouchers/**",
                "/api/settings")
            .hasRole("MANAGER")

            // Kitchen
            .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyRole("MANAGER", "STAFF")
            .requestMatchers(HttpMethod.GET, "/api/orders/table/*/current").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.GET, "/api/tables/**").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.GET, "/api/orders/history").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.GET, "/api/orders/stats").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.GET, "/api/orders/active").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.GET, "/api/kitchen/orders").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.POST, "/api/orders/*/reconcile").hasAnyRole("MANAGER", "STAFF")
            .requestMatchers(HttpMethod.PATCH, "/api/kitchen/items/*/status").hasAnyRole("MANAGER", "STAFF", "CHEF")
            .requestMatchers(HttpMethod.PATCH, "/api/kitchen/items/*/prepared").hasAnyRole("MANAGER", "STAFF", "CHEF")

            .requestMatchers(HttpMethod.PATCH,
                "/api/categories/**",
                "/api/menu-items/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/users/*/reset-password",
                "/api/users/**",
                "/api/orders/**")
            .hasAnyRole("MANAGER")

            .requestMatchers(HttpMethod.DELETE,
                "/api/categories/**",
                "/api/menu-items/**",
                "/api/tables/**",
                "/api/vouchers/**",
                "/api/combos/**",
                "/api/combos/*/items")
            .hasRole("MANAGER")
            .requestMatchers(HttpMethod.PATCH, "/api/combos/**").hasRole("MANAGER")

            // revenue / stats
            .requestMatchers(HttpMethod.GET, "/api/stats/**").hasAnyRole("MANAGER", "STAFF", "CHEF")

            // voucher management
            .requestMatchers(HttpMethod.GET, "/api/vouchers/**").hasAnyRole("MANAGER", "STAFF")

            // Order item administration
            .requestMatchers(HttpMethod.DELETE, "/api/orders/items/*").hasAnyRole("MANAGER", "STAFF")

            // Allow /error to avoid error loops
            .requestMatchers("/error").permitAll()

            // Others require authentication
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Creates and configures an AuthenticationManager bean.
   * 
   * @param cfg AuthenticationConfiguration object
   * @return AuthenticationManager object
   * @throws Exception if an error occurs
   */
  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  /**
   * Creates and configures a CorsConfigurationSource bean.
   * Allowed origins are read from app.cors.allowed-origins (comma-separated).
   *
   * @return CorsConfigurationSource object
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  private static List<String> parseAllowedOrigins(String origins) {
    if (!StringUtils.hasText(origins)) {
      return List.of("http://localhost:5173", "https://order-by-qr.vercel.app");
    }
    return List.of(origins.split("\\s*,\\s*"));
  }
}
