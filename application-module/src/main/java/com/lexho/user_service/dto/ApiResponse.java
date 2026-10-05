package com.lexho.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lexho.user_service.lexho.dto.LexhoDtos;

public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private String token;
    private String timestamp;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = java.time.OffsetDateTime.now().toString();
    }

    public ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = java.time.OffsetDateTime.now().toString();
    }

    public ApiResponse(boolean success, String message, T data, String token) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.token = token;
        this.timestamp = java.time.OffsetDateTime.now().toString();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message, T data, String token) {
        return new ApiResponse<>(true, message, data, token);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(java.time.OffsetDateTime.now().toString());
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    @JsonProperty("accessToken")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getAccessToken() {
        return token;
    }

    @JsonProperty("userId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getUserId() {
        LexhoDtos.AuthVerifyResponse authData = authData();
        return authData == null ? null : authData.userId();
    }

    @JsonProperty("partnerId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getPartnerId() {
        LexhoDtos.AuthVerifyResponse authData = authData();
        return authData == null ? null : authData.partnerId();
    }

    @JsonProperty("providerId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getProviderId() {
        LexhoDtos.AuthVerifyResponse authData = authData();
        return authData == null ? null : authData.providerId();
    }

    @JsonProperty("role")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getRole() {
        LexhoDtos.AuthVerifyResponse authData = authData();
        return authData == null ? null : authData.role();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    private LexhoDtos.AuthVerifyResponse authData() {
        if (data instanceof LexhoDtos.AuthVerifyResponse authVerifyResponse) {
            return authVerifyResponse;
        }
        return null;
    }
}
