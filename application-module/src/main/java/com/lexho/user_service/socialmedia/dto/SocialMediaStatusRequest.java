package com.lexho.user_service.socialmedia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialMediaStatusRequest {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
