package com.lexho.user_service.chat.model;

import com.lexho.user_service.chat.enums.ChatParticipantType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public record ChatActor(ChatParticipantType participantType, Long participantId) {

    public static final String AUTHENTICATED_CHAT_ACTOR_REQUIRED = "Authenticated chat actor is required";

    public static ChatActor fromRole(String role, Long userId) {
        if (role == null || userId == null) {
            throw new IllegalArgumentException(AUTHENTICATED_CHAT_ACTOR_REQUIRED);
        }
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            return new ChatActor(ChatParticipantType.CUSTOMER, userId);
        }
        if ("PARTNER".equalsIgnoreCase(role)) {
            return new ChatActor(ChatParticipantType.PARTNER, userId);
        }
        throw new IllegalArgumentException("Unsupported chat role: " + role);
    }

    public static ChatActor fromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            throw new IllegalArgumentException(AUTHENTICATED_CHAT_ACTOR_REQUIRED);
        }
        Long userId = authentication.getDetails() instanceof Long id ? id : null;
        if (userId == null) {
            throw new IllegalArgumentException(AUTHENTICATED_CHAT_ACTOR_REQUIRED);
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .map(authority -> authority.substring("ROLE_".length()))
                .orElseThrow(() -> new IllegalArgumentException(AUTHENTICATED_CHAT_ACTOR_REQUIRED));
        return fromRole(role, userId);
    }

    public String principalName() {
        return participantType.name() + ":" + participantId;
    }
}
