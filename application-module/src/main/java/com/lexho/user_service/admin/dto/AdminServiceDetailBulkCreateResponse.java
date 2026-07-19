package com.lexho.user_service.admin.dto;

import java.util.List;

public class AdminServiceDetailBulkCreateResponse {
    private String message;
    private List<AdminServiceDetailResponse> data;

    public AdminServiceDetailBulkCreateResponse(String message, List<AdminServiceDetailResponse> data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AdminServiceDetailResponse> getData() {
        return data;
    }

    public void setData(List<AdminServiceDetailResponse> data) {
        this.data = data;
    }
}
