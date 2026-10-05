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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
    void customerCanSubscribeToOwnedBookingTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/customer/bookings/42");
        accessor.setUser(auth("CUSTOMER:11", 11L, Role.CUSTOMER));
        when(chatService.hasBookingAccess(42L,
                new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.CUSTOMER, 11L)))
                .thenReturn(true);

        interceptor.preSend(message(accessor), null);
    }

    @Test
    void customerCannotSubscribeToAnotherCustomersBookingTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/customer/bookings/42/eta");
        accessor.setUser(auth("CUSTOMER:11", 11L, Role.CUSTOMER));
        when(chatService.hasBookingAccess(42L,
                new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.CUSTOMER, 11L)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void assignedPartnerCanSubscribeToBookingEtaButUnrelatedPartnerCannot() {
        StompHeaderAccessor allowed = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        allowed.setDestination("/topic/navigation/eta/42");
        allowed.setUser(auth("PARTNER:77", 77L, Role.PARTNER));
        when(chatService.hasBookingAccess(42L,
                new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 77L)))
                .thenReturn(true);

        interceptor.preSend(message(allowed), null);

        StompHeaderAccessor denied = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        denied.setDestination("/topic/customer/bookings/42");
        denied.setUser(auth("PARTNER:78", 78L, Role.PARTNER));
        when(chatService.hasBookingAccess(42L,
                new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 78L)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(denied), null));
    }

    @Test
    void customerCannotSubscribeToAnotherUsersNotificationTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/notifications/12");
        accessor.setUser(auth("CUSTOMER:11", 11L, Role.CUSTOMER));

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void adminNotificationTopicRequiresAdminRole() {
        StompHeaderAccessor customer = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        customer.setDestination("/topic/admin/notifications");
        customer.setUser(auth("CUSTOMER:11", 11L, Role.CUSTOMER));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(customer), null));

        Claims claims = new DefaultClaims();
        claims.put("userId", 1);
        claims.put("role", Role.ADMIN.name());
        when(jwtUtil.validateToken("admin-token")).thenReturn(claims);
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.addNativeHeader("Authorization", "Bearer admin-token");
        UsernamePasswordAuthenticationToken admin =
                (UsernamePasswordAuthenticationToken) StompHeaderAccessor.wrap(
                        cast(interceptor.preSend(message(connect), null))).getUser();

        StompHeaderAccessor allowed = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        allowed.setDestination("/topic/admin/notifications");
        allowed.setUser(admin);
        interceptor.preSend(message(allowed), null);
    }

    @Test
    void partnerCanSubscribeOnlyToOwnDispatchTopic() {
        StompHeaderAccessor allowed = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        allowed.setDestination("/topic/partner/dispatch/77");
        allowed.setUser(auth("PARTNER:77", 77L, Role.PARTNER));
        interceptor.preSend(message(allowed), null);

        StompHeaderAccessor denied = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        denied.setDestination("/topic/partner/dispatch/78");
        denied.setUser(auth("PARTNER:77", 77L, Role.PARTNER));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(denied), null));
    }

    @Test
    void partnerDispatchTopicRejectsCustomersAndPartnerBroadcastSubscribers() {
        StompHeaderAccessor customer = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        customer.setDestination("/topic/partner/dispatch/77");
        customer.setUser(auth("CUSTOMER:77", 77L, Role.CUSTOMER));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(customer), null));

        StompHeaderAccessor partnerBroadcast = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        partnerBroadcast.setDestination("/topic/partner/dispatch");
        partnerBroadcast.setUser(auth("PARTNER:77", 77L, Role.PARTNER));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(partnerBroadcast), null));
    }

    @Test
    void adminCanSubscribeToPartnerDispatchTopics() {
        StompHeaderAccessor broadcast = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        broadcast.setDestination("/topic/partner/dispatch");
        broadcast.setUser(auth("ADMIN:1", 1L, Role.ADMIN));
        interceptor.preSend(message(broadcast), null);

        StompHeaderAccessor specific = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        specific.setDestination("/topic/partner/dispatch/77");
        specific.setUser(auth("ADMIN:1", 1L, Role.ADMIN));
        interceptor.preSend(message(specific), null);
    }

    @Test
    void connectShouldRejectMissingToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void connectShouldRejectMalformedClaimsAsAccessDenied() {
        Claims claims = new DefaultClaims();
        when(jwtUtil.validateToken("token")).thenReturn(claims);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer token");

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void disconnectShouldPassWithoutAuthentication() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);

        Message<byte[]> result = cast(interceptor.preSend(message(accessor), null));

        assertNotNull(result);
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
