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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
        // static (css/js/images)
        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
        // file HTML cụ thể
        .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/forget-password.html").permitAll()
    
        .requestMatchers(new AntPathRequestMatcher("/**/*.html")).permitAll()

        // UI admin tĩnh
        .requestMatchers("/admin/**").permitAll()

        // websocket & auth
        .requestMatchers("/ws/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/register").permitAll()
        .requestMatchers("/api/auth/**").permitAll()

        // public GET
        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/menu/**", "/api/tables/**").permitAll()
        // khách tạo đơn
        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()

        // admin
        .requestMatchers(HttpMethod.POST,   "/api/categories/**", "/api/menu/**", "/api/tables/**").hasRole("MANAGER")
        .requestMatchers(HttpMethod.PUT,    "/api/categories/**", "/api/menu/**", "/api/tables/**").hasRole("MANAGER")
        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/menu/**", "/api/tables/**").hasRole("MANAGER")

        // revenue
        .requestMatchers(HttpMethod.GET, "/api/stats/**").hasAnyRole("MANAGER","STAFF")

        // cho phép /error để không dính vòng lặp lỗi
        .requestMatchers("/error").permitAll()

        // cho phép truy cập thư mục uploads
         .requestMatchers("/uploads/**").permitAll()

        // còn lại cần đăng nhập
        .anyRequest().authenticated()
      )
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(
      "http://localhost:*",
      "http://127.0.0.1:*",
      "http://192.168.1.*:*",
      "http://10.102.190.*:*",
      "http://10.60.72.60",
      "http://10.50.252.193"
    ));
    c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type","Accept"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
