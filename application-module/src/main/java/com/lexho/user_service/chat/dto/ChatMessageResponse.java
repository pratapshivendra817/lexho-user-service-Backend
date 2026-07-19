package com.lexho.user_service.chat.dto;

import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageResponse {
    private final Long messageId;
    private final Long roomId;
    private final ChatParticipantType senderType;
    private final Long senderId;
    private final ChatMessageType messageType;
    private final String content;
    private final ChatMessageDeliveryStatus deliveryStatus;
    private final Instant deliveredAt;
    private final Instant readAt;
    private final boolean deleted;
    private final Instant createdAt;
    private final Instant updatedAt;
}
