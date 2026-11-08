package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.shared.security.JwtService;
import com.qros.shared.security.TokenBlacklistService;
import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

class WebSocketConfigTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final WebSocketConfig config =
            new WebSocketConfig(jwtService, userDetailsService, new AppProperties(), tokenBlacklistService);

    @Test
    void protectedTopicRequiresAuthenticatedUser() {
        Message<byte[]> message = subscribeMessage("/topic/kitchen", null);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void protectedTopicAllowsConfiguredRole() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "chef", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CHEF")));
        Message<byte[]> message = subscribeMessage("/topic/kitchen", auth);

        assertThatCode(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void publicTopicAllowsAnonymousSubscribe() {
        Message<byte[]> message = subscribeMessage("/topic/menu", null);

        assertThatCode(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void clientSendIsDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
        accessor.setDestination("/topic/tables");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Client WebSocket messages are not allowed");
    }

    @Test
    void staffCannotSubscribeToKitchenTopic() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "staff", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STAFF")));
        Message<byte[]> message = subscribeMessage("/topic/kitchen", auth);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Insufficient role");
    }

    @Test
    void unknownTopicIsDeniedByDefault() {
        Message<byte[]> message = subscribeMessage("/topic/internal-new-feature", null);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Subscription is not allowed");
    }

    @Test
    void connectWithBlacklistedTokenThrowsAccessDenied() {
        String token = "blacklisted-ws-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setMessageTypeIfNotSet(SimpMessageType.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractTokenType(token)).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void connectWithTokenIssuedBeforePasswordChangeThrowsAccessDenied() {
        String token = "stale-ws-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setMessageTypeIfNotSet(SimpMessageType.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractTokenType(token)).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenReturn("user@example.com");

        LocalDateTime changeTime = AppTime.now().truncatedTo(ChronoUnit.SECONDS);
        TestUserDetails user = mock(TestUserDetails.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.isAccountNonLocked()).thenReturn(true);
        when(user.isAccountNonExpired()).thenReturn(true);
        when(user.isCredentialsNonExpired()).thenReturn(true);
        when(user.getPasswordChangedAt()).thenReturn(changeTime);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);

        Date issuedAt = new Date(
                changeTime.minusSeconds(10).atZone(AppTime.ZONE).toInstant().toEpochMilli());
        when(jwtService.extractIssuedAt(token)).thenReturn(issuedAt);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("password change");
    }

    @Test
    void connectWithInactiveUserThrowsAccessDenied() {
        String token = "inactive-ws-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setMessageTypeIfNotSet(SimpMessageType.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractTokenType(token)).thenReturn("access");
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenReturn("inactive@example.com");

        TestUserDetails user = mock(TestUserDetails.class);
        when(user.isEnabled()).thenReturn(false);
        when(userDetailsService.loadUserByUsername("inactive@example.com")).thenReturn(user);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor()
                        .preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not active");
    }

    private interface TestUserDetails
            extends org.springframework.security.core.userdetails.UserDetails,
                    com.qros.shared.security.PasswordVersionedUserDetails {}

    private Message<byte[]> subscribeMessage(String destination, UsernamePasswordAuthenticationToken auth) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setMessageTypeIfNotSet(SimpMessageType.SUBSCRIBE);
        accessor.setDestination(destination);
        if (auth != null) {
            accessor.setUser(auth);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
