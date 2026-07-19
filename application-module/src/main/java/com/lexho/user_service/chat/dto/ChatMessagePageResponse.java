package com.lexho.user_service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatMessagePageResponse {
    private final List<ChatMessageResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final long unreadCount;
}
