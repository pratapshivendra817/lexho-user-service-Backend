package com.lexho.user_service.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChatRoomRequest {

    @NotNull
    private Long orderId;
}
