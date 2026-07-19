package com.lexho.user_service.chat.config;

import com.lexho.common.enums.Role;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class ChatStompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";

    private final JwtUtil jwtUtil;
    private final ChatService chatService;

    public ChatStompAuthChannelInterceptor(JwtUtil jwtUtil,
                                           @Lazy ChatService chatService) {
        this.jwtUtil = jwtUtil;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw new AccessDeniedException("Unauthorized websocket access");
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor, ChatActor.fromAuthentication(authentication));
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String authHeader = firstNativeHeader(accessor, "Authorization");
        if (authHeader == null) {
            authHeader = firstNativeHeader(accessor, "authorization");
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authorization token is required");
        }

        Claims claims = jwtUtil.validateToken(authHeader.substring(7).trim());
        Long userId = ((Number) claims.get("userId")).longValue();
        String roleClaim = claims.get("role", String.class);
        Role role = Role.valueOf(roleClaim.toUpperCase(Locale.ROOT));
        if (role != Role.CUSTOMER && role != Role.PARTNER) {
            throw new AccessDeniedException("Websocket chat is only available for customer and partner roles");
        }

        ChatActor actor = ChatActor.fromRole(role.name(), userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                actor.principalName(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        authentication.setDetails(userId);
        return authentication;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor, ChatActor actor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return;
        }
        Long roomId = parseLongId(destination.substring(ROOM_TOPIC_PREFIX.length()), "Invalid chat room destination");
        if (!chatService.hasRoomAccess(roomId, actor)) {
            throw new AccessDeniedException("Access denied to chat room subscription");
        }
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        List<String> values = accessor.getNativeHeader(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private Long parseLongId(String rawId, String message) {
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException(message);
        }
    }
}
