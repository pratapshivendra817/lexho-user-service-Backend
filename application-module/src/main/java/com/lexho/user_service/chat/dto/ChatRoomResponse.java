package com.lexho.user_service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ChatRoomResponse {
    private final Long roomId;
    private final Long orderId;
    private final String lastMessagePreview;
    private final Instant lastMessageAt;
    private final long unreadCount;
    private final List<ChatParticipantSummary> participants;
    private final Instant createdAt;
    private final Instant updatedAt;
}
