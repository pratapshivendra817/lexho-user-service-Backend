package com.lexho.user_service.admin.dto;

import com.lexho.user_service.admin.entity.DetailType;

public class AdminServiceDetailResponse {
    private Long id;
    private DetailType type;
    private String itemName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
