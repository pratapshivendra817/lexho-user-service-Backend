package com.lexho.user_service.chat.dto;

import com.lexho.user_service.chat.enums.ChatEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatSocketEvent<T> {
    private final ChatEventType eventType;
    private final Long roomId;
    private final T payload;
    private final Instant occurredAt;
}
