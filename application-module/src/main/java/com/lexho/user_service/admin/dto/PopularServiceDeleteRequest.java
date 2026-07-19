package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.Size;

public class PopularServiceDeleteRequest {

    @Size(max = 500, message = "deleteReason must be at most 500 characters")
    private String deleteReason;

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }
}
