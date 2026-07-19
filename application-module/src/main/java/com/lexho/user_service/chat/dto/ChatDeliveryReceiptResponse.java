package com.lexho.user_service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatDeliveryReceiptResponse {
    private final Long roomId;
    private final Long messageId;
    private final Instant deliveredAt;
}
