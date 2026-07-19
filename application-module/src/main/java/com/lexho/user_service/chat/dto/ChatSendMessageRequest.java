package com.lexho.user_service.chat.dto;

import com.lexho.user_service.chat.enums.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSendMessageRequest {

    @NotNull
    private Long roomId;

    @NotBlank
    private String content;

    @NotNull
    private ChatMessageType messageType = ChatMessageType.TEXT;
}
