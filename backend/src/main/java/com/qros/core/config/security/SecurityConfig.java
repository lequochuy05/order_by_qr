package com.qros.core.config.security;

import static com.qros.core.config.security.SecurityRoutes.*;

import com.qros.shared.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_MANAGER \n"
                + "ROLE_MANAGER > ROLE_CASHIER \n"
                + "ROLE_MANAGER > ROLE_CHEF \n"
                + "ROLE_CASHIER > ROLE_STAFF");
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'self'; object-src 'none'; base-uri 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=(self), unload=(self)")))
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
                        .requestMatchers(HttpMethod.GET, ADMIN_GET)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, ADMIN_POST)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ADMIN_PUT)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ADMIN_DELETE)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, MANAGER_GET)
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, CASHIER_ORDER_HISTORY_GET)
                        .hasAnyRole("MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.GET, ANALYTICS_GET)
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, STAFF_READ_GET)
                        .hasAnyRole("MANAGER", "CHEF", "STAFF")
                        .requestMatchers(HttpMethod.GET, OPERATION_GET)
                        .hasAnyRole("MANAGER", "STAFF", "CHEF")
                        .requestMatchers(HttpMethod.GET, KITCHEN_GET)
                        .hasAnyRole("MANAGER", "CHEF")
                        .requestMatchers(HttpMethod.PATCH, KITCHEN_PATCH)
                        .hasAnyRole("MANAGER", "CHEF")
                        .requestMatchers(HttpMethod.DELETE, KITCHEN_DELETE)
                        .hasAnyRole("MANAGER", "CHEF")
                        .requestMatchers(HttpMethod.POST, STAFF_AI_POST)
                        .hasAnyRole("MANAGER", "STAFF", "CHEF")
                        .requestMatchers(HttpMethod.POST, STAFF_OPERATION_POST)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.PATCH, STAFF_OPERATION_PATCH)
                        .hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, STAFF_OPERATION_DELETE)
                        .hasAnyRole("MANAGER", "STAFF", "CHEF")
                        .requestMatchers(PAYMENT_ROUTES)
                        .hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.POST, INVENTORY_OPERATION_POST)
                        .hasAnyRole("MANAGER", "CHEF")
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
