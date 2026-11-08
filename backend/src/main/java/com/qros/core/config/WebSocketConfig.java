package com.qros.core.config;

import com.qros.shared.security.JwtService;
import com.qros.shared.security.PasswordVersionedUserDetails;
import com.qros.shared.security.TokenBlacklistService;
import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig - Configures WebSocket message broker and STOMP endpoints.
 * Enables real-time communication for order updates.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Set<String> PUBLIC_TOPICS =
            Set.of("/topic/tables", "/topic/menu", "/topic/categories", "/topic/combos", "/topic/settings");

    /** Topics requiring authenticated subscriptions. Unknown topics are denied. */
    private static final Map<String, Set<String>> PROTECTED_TOPICS = Map.of(
            "/topic/orders", Set.of("ROLE_MANAGER", "ROLE_STAFF", "ROLE_CHEF"),
            "/topic/kitchen", Set.of("ROLE_MANAGER", "ROLE_CHEF"),
            "/topic/users", Set.of("ROLE_MANAGER"),
            "/topic/vouchers", Set.of("ROLE_MANAGER", "ROLE_STAFF"),
            "/topic/promotions", Set.of("ROLE_MANAGER", "ROLE_STAFF"),
            "/topic/inventory", Set.of("ROLE_MANAGER", "ROLE_STAFF", "ROLE_CHEF"));

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AppProperties appProperties;
    private final TokenBlacklistService tokenBlacklistService;

    public WebSocketConfig(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            AppProperties appProperties,
            TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.appProperties = appProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Registers STOMP endpoints for WebSocket connections.
     *
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(parseAllowedOrigins(appProperties.getCors().getAllowedOrigins()))
                .withSockJS();
    }

    /**
     * Parses a comma-separated origin string into an array, trimming whitespace.
     */
    private static String[] parseAllowedOrigins(String origins) {
        if (!org.springframework.util.StringUtils.hasText(origins)) {
            return new String[] {"http://localhost:5173", "https://localhost", "https://wqros.vercel.app"};
        }
        return origins.split("\\s*,\\s*");
    }

    /**
     * Configures the message broker for routing messages.
     * Sets up STOMP heartbeats (10s each direction) so that dropped connections
     * are detected promptly rather than waiting for the next message send.
     *
     * @param registry Message broker registry
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] {10000, 10000})
                .setTaskScheduler(heartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Dedicated scheduler for STOMP heartbeat frames so broker heartbeats
     * run on their own thread and don't contend with application tasks.
     */
    @Bean
    public TaskScheduler heartbeatScheduler() {
        return new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
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

                SimpMessageType messageType = accessor.getMessageType();
                if (SimpMessageType.CONNECT.equals(messageType)) {
                    authenticateConnect(accessor);
                    return message;
                }

                if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
                    authorizeSubscription(accessor);
                    return message;
                }

                if (SimpMessageType.DISCONNECT.equals(messageType)
                        || SimpMessageType.UNSUBSCRIBE.equals(messageType)
                        || SimpMessageType.HEARTBEAT.equals(messageType)
                        || messageType == null) {
                    return message;
                }

                throw new AccessDeniedException("Client WebSocket messages are not allowed");
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

        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new AccessDeniedException("WebSocket token has been revoked");
        }

        String email = jwtService.extractSubject(token);
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException ex) {
            throw new AccessDeniedException("WebSocket user account not found");
        }

        if (!isUsableAccount(userDetails)) {
            throw new AccessDeniedException("WebSocket user account is not active");
        }

        if (!isFreshForPasswordVersion(token, userDetails)) {
            throw new AccessDeniedException("WebSocket token has expired due to password change");
        }

        if (userDetails instanceof com.qros.modules.user.model.User user && user.isMustChangePassword()) {
            throw new AccessDeniedException("Password change required before connecting to WebSocket");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);
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
        long passwordChangedAtSec = passwordChangedAt.atZone(AppTime.ZONE).toEpochSecond();
        return issuedAtSec >= passwordChangedAtSec;
    }

    /**
     * Authorizes the WebSocket subscription.
     *
     * @param accessor StompHeaderAccessor object
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("WebSocket destination is required");
        }

        if (PUBLIC_TOPICS.contains(destination)) {
            return;
        }

        if (!PROTECTED_TOPICS.containsKey(destination)) {
            throw new AccessDeniedException("Subscription is not allowed for topic " + destination);
        }

        if (!(accessor.getUser() instanceof Authentication auth) || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required for topic " + destination);
        }

        Set<String> allowedRoles = PROTECTED_TOPICS.get(destination);
        boolean allowed =
                auth.getAuthorities().stream().anyMatch(authority -> allowedRoles.contains(authority.getAuthority()));
        if (!allowed) {
            throw new AccessDeniedException("Insufficient role for topic " + destination);
        }
    }

    private boolean isUsableAccount(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }
}
