package com.sacmauquan.qrordering.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

import com.sacmauquan.qrordering.security.JwtService;

class WebSocketConfigTest {
    private final WebSocketConfig config = new WebSocketConfig(mock(JwtService.class), mock(UserDetailsService.class));

    @Test
    void protectedTopicRequiresAuthenticatedUser() {
        Message<byte[]> message = subscribeMessage("/topic/kitchen", null);

        assertThatThrownBy(() -> config.webSocketAuthInterceptor().preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void protectedTopicAllowsConfiguredRole() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "chef", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CHEF")));
        Message<byte[]> message = subscribeMessage("/topic/kitchen", auth);

        assertThatCode(() -> config.webSocketAuthInterceptor().preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void publicTopicAllowsAnonymousSubscribe() {
        Message<byte[]> message = subscribeMessage("/topic/menu", null);

        assertThatCode(() -> config.webSocketAuthInterceptor().preSend(message, mock(org.springframework.messaging.MessageChannel.class)))
                .doesNotThrowAnyException();
    }

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
