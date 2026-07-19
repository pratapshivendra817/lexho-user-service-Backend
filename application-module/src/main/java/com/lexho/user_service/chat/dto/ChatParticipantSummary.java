package com.lexho.user_service.chat.dto;

import com.lexho.user_service.chat.enums.ChatParticipantType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatParticipantSummary {
    private final ChatParticipantType participantType;
    private final Long participantId;
}
