package com.lexho.user_service.admin.dto;

import com.lexho.user_service.admin.entity.DetailType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AdminServiceDetailBulkRequest {

    @NotNull(message = "type is required")
    private DetailType type;

    @NotEmpty(message = "items cannot be empty")
    private List<@Size(min = 1, max = 255, message = "item must be between 1 and 255 characters") String> items;

    public DetailType getType() {
        return type;
    }

    public void setType(DetailType type) {
        this.type = type;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
