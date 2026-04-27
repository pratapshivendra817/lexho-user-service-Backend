package com.lexho.user_service.dto;

public class AdminLoginResponse {

    private String email;
    private int otpExpiresInSeconds;

    public AdminLoginResponse() {
    }

    public AdminLoginResponse(String email, int otpExpiresInSeconds) {
        this.email = email;
        this.otpExpiresInSeconds = otpExpiresInSeconds;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getOtpExpiresInSeconds() {
        return otpExpiresInSeconds;
    }

    public void setOtpExpiresInSeconds(int otpExpiresInSeconds) {
        this.otpExpiresInSeconds = otpExpiresInSeconds;
    }
}
