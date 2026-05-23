package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PopularServiceUpsertRequest {

    @NotNull(message = "serviceId is required")
    private Long serviceId;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "priority is required")
    @Min(value = 1, message = "priority must be at least 1")
    private Integer priority;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
