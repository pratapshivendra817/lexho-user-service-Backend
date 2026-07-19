package com.lexho.user_service.chat.dto;

import com.lexho.user_service.chat.enums.ChatParticipantType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatTypingEventResponse {
    private final Long roomId;
    private final ChatParticipantType participantType;
    private final Long participantId;
    private final Instant occurredAt;
}
