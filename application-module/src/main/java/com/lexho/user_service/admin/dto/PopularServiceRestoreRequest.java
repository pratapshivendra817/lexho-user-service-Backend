package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.Min;

public class PopularServiceRestoreRequest {

    @Min(value = 1, message = "priority must be at least 1")
    private Integer priority;

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
