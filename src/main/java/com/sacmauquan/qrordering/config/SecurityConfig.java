// src/main/java/com/sacmauquan/qrordering/config/SecurityConfig.java
package com.sacmauquan.qrordering.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sacmauquan.qrordering.security.JwtAuthFilter;

import java.util.List;

/**
 * Configure security settings for the application.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

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
            .requestMatchers("/api/auth/**", "/ws/**", "/error").permitAll()
            // login
            .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()

            // Public GET
            .requestMatchers(HttpMethod.GET,
                "/api/categories/**",
                "/api/menu/**",
                "/api/tables/**",
                "/api/combos/**",
                "/api/orders/table/*/current",
                "/api/combos/active",
                "/api/combos/*/items",
                "/api/recommendations/**")
            .permitAll()

            // Public Voucher Validation only
            .requestMatchers(HttpMethod.GET, "/api/vouchers/validate").permitAll()
            // Guest order creation
            .requestMatchers(HttpMethod.POST, "/api/orders/**").permitAll()
            // AI Customer Assistant (public chat)
            .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()

            // PayOS webhook
            .requestMatchers(HttpMethod.POST, "/api/webhooks/**").permitAll()

            .requestMatchers(HttpMethod.POST, "/api/payments/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/payments/**").permitAll()

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

            // Kitchen
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

            // voucher management
            .requestMatchers(HttpMethod.GET, "/api/vouchers/**").hasAnyRole("MANAGER", "STAFF")

            // upload avatar (Authenticated users only)
            .requestMatchers(HttpMethod.POST, "/api/users/*/avatar").authenticated()

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
   * 
   * @return CorsConfigurationSource object
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(
        "http://localhost:5173",
        "https://order-by-qr.vercel.app"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
