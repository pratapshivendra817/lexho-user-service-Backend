package com.lexho.user_service.chat.config;

import com.lexho.common.enums.Role;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
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
    private static final String CUSTOMER_BOOKING_TOPIC_PREFIX = "/topic/customer/bookings/";
    private static final String ADMIN_BOOKING_TOPIC_PREFIX = "/topic/admin/bookings/";
    private static final String NOTIFICATION_TOPIC_PREFIX = "/topic/notifications/";
    private static final String NAVIGATION_ETA_TOPIC_PREFIX = "/topic/navigation/eta/";
    private static final String PARTNER_DISPATCH_TOPIC = "/topic/partner/dispatch";
    private static final String PARTNER_DISPATCH_TOPIC_PREFIX = PARTNER_DISPATCH_TOPIC + "/";
    private static final String ADMIN_NOTIFICATION_TOPIC = "/topic/admin/notifications";

    private final JwtUtil jwtUtil;
    private final ChatService chatService;

    public ChatStompAuthChannelInterceptor(JwtUtil jwtUtil,
                                           @Lazy ChatService chatService) {
        this.jwtUtil = jwtUtil;
        this.chatService = chatService;
    }

    @Override
    @Nullable
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            return message;
        }

        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw new AccessDeniedException("Unauthorized websocket access");
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor, authentication);
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

        Claims claims;
        try {
            claims = jwtUtil.validateToken(authHeader.substring(7).trim());
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("Invalid or expired token", exception);
        }
        Long userId = userIdFrom(claims);
        Role role = roleFrom(claims.get("role", String.class));
        if (role != Role.CUSTOMER && role != Role.PARTNER && role != Role.ADMIN) {
            throw new AccessDeniedException("Websocket role is not permitted");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                role.name() + ":" + userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        authentication.setDetails(userId);
        return authentication;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor,
                                       UsernamePasswordAuthenticationToken authentication) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Websocket subscription destination is required");
        }

        Long userId = authenticatedUserId(authentication);
        Role role = authenticatedRole(authentication);
        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            if (role == Role.ADMIN) {
                throw new AccessDeniedException("Admins cannot subscribe to chat rooms");
            }
            Long roomId = parseLongId(destination.substring(ROOM_TOPIC_PREFIX.length()),
                    "Invalid chat room destination");
            ChatActor actor = ChatActor.fromRole(role.name(), userId);
            if (!chatService.hasRoomAccess(roomId, actor)) {
                throw new AccessDeniedException("Access denied to chat room subscription");
            }
            return;
        }
        if (destination.equals(ADMIN_NOTIFICATION_TOPIC)) {
            requireRole(role, Role.ADMIN);
            return;
        }
        if (destination.startsWith(NOTIFICATION_TOPIC_PREFIX)) {
            if (role == Role.ADMIN) {
                throw new AccessDeniedException("Admin notification topic is required");
            }
            Long recipientId = parseLongId(destination.substring(NOTIFICATION_TOPIC_PREFIX.length()),
                    "Invalid notification destination");
            if (!userId.equals(recipientId)) {
                throw new AccessDeniedException("Access denied to notification subscription");
            }
            return;
        }
        if (destination.startsWith(CUSTOMER_BOOKING_TOPIC_PREFIX)) {
            requireBookingAccess(destination.substring(CUSTOMER_BOOKING_TOPIC_PREFIX.length()), role, userId);
            return;
        }
        if (destination.startsWith(NAVIGATION_ETA_TOPIC_PREFIX)) {
            requireBookingAccess(destination.substring(NAVIGATION_ETA_TOPIC_PREFIX.length()), role, userId);
            return;
        }
        if (destination.startsWith(ADMIN_BOOKING_TOPIC_PREFIX)) {
            requireRole(role, Role.ADMIN);
            parseBookingId(destination.substring(ADMIN_BOOKING_TOPIC_PREFIX.length()));
            return;
        }
        if (destination.equals(PARTNER_DISPATCH_TOPIC)) {
            requireRole(role, Role.ADMIN);
            return;
        }
        if (destination.startsWith(PARTNER_DISPATCH_TOPIC_PREFIX)) {
            requirePartnerDispatchAccess(destination.substring(PARTNER_DISPATCH_TOPIC_PREFIX.length()), role, userId);
            return;
        }
        if (destination.startsWith("/topic/")) {
            throw new AccessDeniedException("Unrecognized private topic");
        }
        if (!destination.startsWith("/user/")) {
            throw new AccessDeniedException("Websocket subscription destination is not permitted");
        }
    }

    private void requireBookingAccess(String rawBookingId, Role role, Long userId) {
        Long bookingId = parseBookingId(rawBookingId);
        if (role == Role.ADMIN) {
            return;
        }
        ChatActor actor = ChatActor.fromRole(role.name(), userId);
        if (!chatService.hasBookingAccess(bookingId, actor)) {
            throw new AccessDeniedException("Access denied to booking subscription");
        }
    }

    private void requirePartnerDispatchAccess(String rawPartnerId, Role role, Long userId) {
        Long partnerId = parseLongId(rawPartnerId, "Invalid partner dispatch destination");
        if (role == Role.ADMIN) {
            return;
        }
        requireRole(role, Role.PARTNER);
        if (!userId.equals(partnerId)) {
            throw new AccessDeniedException("Access denied to partner dispatch subscription");
        }
    }

    private Long parseBookingId(String rawBookingId) {
        String id = rawBookingId;
        int suffix = id.indexOf('/');
        if (suffix >= 0) {
            String trailingPath = id.substring(suffix);
            if (!"/eta".equals(trailingPath)) {
                throw new AccessDeniedException("Invalid booking destination");
            }
            id = id.substring(0, suffix);
        }
        return parseLongId(id, "Invalid booking destination");
    }

    private Long authenticatedUserId(UsernamePasswordAuthenticationToken authentication) {
        if (authentication.getDetails() instanceof Long userId) {
            return userId;
        }
        throw new AccessDeniedException("Invalid websocket principal");
    }

    private Role authenticatedRole(UsernamePasswordAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .map(this::roleFrom)
                .orElseThrow(() -> new AccessDeniedException("Invalid websocket principal"));
    }

    private void requireRole(Role actual, Role expected) {
        if (actual != expected) {
            throw new AccessDeniedException("Access denied to websocket destination");
        }
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        List<String> values = accessor.getNativeHeader(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private Long userIdFrom(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AccessDeniedException("Invalid token user id");
    }

    private Role roleFrom(String roleClaim) {
        if (roleClaim == null || roleClaim.isBlank()) {
            throw new AccessDeniedException("Invalid token role");
        }
        try {
            return Role.valueOf(roleClaim.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid token role", exception);
        }
    }

    private Long parseLongId(String rawId, String message) {
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException(message);
        }
    }
}
