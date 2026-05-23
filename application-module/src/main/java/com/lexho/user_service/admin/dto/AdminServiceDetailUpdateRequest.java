package com.lexho.user_service.admin.dto;

import com.lexho.user_service.admin.entity.DetailType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminServiceDetailUpdateRequest {

    @NotNull(message = "type is required")
    private DetailType type;

    @NotBlank(message = "itemName is required")
    @Size(max = 255, message = "itemName must be at most 255 characters")
    private String itemName;

    public DetailType getType() {
        return type;
    }

    public void setType(DetailType type) {
        this.type = type;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
