package com.sacmauquan.qrordering.config;

import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

import com.sacmauquan.qrordering.security.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * WebSocketConfig - Configures WebSocket message broker and STOMP endpoints.
 * Enables real-time communication for order updates.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Map<String, Set<String>> PROTECTED_TOPICS = Map.of(
            "/topic/kitchen", Set.of("ROLE_MANAGER", "ROLE_STAFF", "ROLE_CHEF"),
            "/topic/users", Set.of("ROLE_MANAGER"),
            "/topic/vouchers", Set.of("ROLE_MANAGER", "ROLE_STAFF"));

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Registers STOMP endpoints for WebSocket connections.
     * 
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "https://order-by-qr.vercel.app")
                .withSockJS();
    }

    /**
     * Configures the message broker for routing messages.
     * 
     * @param registry Message broker registry
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Configures the client inbound channel for WebSocket authentication.
     * 
     * @param registration Channel registration
     */
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor());
    }

    /**
     * Creates and configures a ChannelInterceptor bean for WebSocket
     * authentication.
     * 
     * @return ChannelInterceptor object
     */
    @Bean
    ChannelInterceptor webSocketAuthInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (SimpMessageType.CONNECT.equals(accessor.getMessageType())) {
                    authenticateConnect(accessor);
                }

                if (SimpMessageType.SUBSCRIBE.equals(accessor.getMessageType())) {
                    authorizeSubscription(accessor);
                }

                return message;
            }
        };
    }

    /**
     * Authenticates the WebSocket connection.
     * 
     * @param accessor StompHeaderAccessor object
     */
    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isValid(token) || !"access".equals(jwtService.extractTokenType(token))) {
            throw new AccessDeniedException("Invalid WebSocket token");
        }

        String email = jwtService.extractSubject(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);
    }

    /**
     * Authorizes the WebSocket subscription.
     * 
     * @param accessor StompHeaderAccessor object
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !PROTECTED_TOPICS.containsKey(destination)) {
            return;
        }

        if (!(accessor.getUser() instanceof Authentication auth) || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required for topic " + destination);
        }

        Set<String> allowedRoles = PROTECTED_TOPICS.get(destination);
        boolean allowed = auth.getAuthorities().stream()
                .anyMatch(authority -> allowedRoles.contains(authority.getAuthority()));
        if (!allowed) {
            throw new AccessDeniedException("Insufficient role for topic " + destination);
        }
    }
}
