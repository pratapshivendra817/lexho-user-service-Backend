package com.lexho.user_service.review.dto;

import jakarta.validation.constraints.Size;

public class ModerationActionRequest {
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    public String reason;
}
