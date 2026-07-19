package com.lexho.user_service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomPageResponse {
    private final List<ChatRoomResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
