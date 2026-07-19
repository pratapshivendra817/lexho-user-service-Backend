package com.lexho.user_service.chat.config;

import com.lexho.common.enums.Role;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStompAuthChannelInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final ChatService chatService = mock(ChatService.class);
    private final ChatStompAuthChannelInterceptor interceptor = new ChatStompAuthChannelInterceptor(jwtUtil, chatService);

    @Test
    void connectShouldAuthenticateCustomer() {
        Claims claims = new DefaultClaims();
        claims.put("userId", 11);
        claims.put("role", Role.CUSTOMER.name());
        when(jwtUtil.validateToken("token")).thenReturn(claims);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer token");

        Message<byte[]> result = cast(interceptor.preSend(message(accessor), null));
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertNotNull(resultAccessor.getUser());
        Authentication authentication = (Authentication) resultAccessor.getUser();
        ChatActor actor = ChatActor.fromAuthentication(authentication);
        assertEquals("CUSTOMER:11", actor.principalName());
    }

    @Test
    void subscribeShouldRejectRoomAccessWhenActorIsNotParticipant() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/rooms/99");
        accessor.setUser(auth("PARTNER:77", 77L, Role.PARTNER));
        when(chatService.hasRoomAccess(99L, new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 77L)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void connectShouldRejectMissingToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    private Authentication auth(String name, Long userId, Role role) {
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        name,
                        null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
                );
        authentication.setDetails(userId);
        return authentication;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @SuppressWarnings("unchecked")
    private Message<byte[]> cast(Message<?> message) {
        return (Message<byte[]>) message;
    }
}
