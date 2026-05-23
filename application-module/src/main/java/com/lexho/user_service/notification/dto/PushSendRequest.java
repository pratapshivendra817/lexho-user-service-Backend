package com.lexho.user_service.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSendRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must be at most 2000 characters")
    private String message;

    @Valid
    private PushSendData data;

    @Getter
    @Setter
    public static class PushSendData {
        @NotBlank(message = "data.redirectType is required")
        private String redirectType;

        private String bookingId;
    }
}
