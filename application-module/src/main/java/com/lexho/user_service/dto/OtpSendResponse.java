package com.lexho.user_service.dto;

public class OtpSendResponse {

    private String mobile;
    private int expiresInSeconds;
    private int resendAfterSeconds;

    public OtpSendResponse() {
    }

    public OtpSendResponse(String mobile, int expiresInSeconds, int resendAfterSeconds) {
        this.mobile = mobile;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public int getResendAfterSeconds() {
        return resendAfterSeconds;
    }

    public void setResendAfterSeconds(int resendAfterSeconds) {
        this.resendAfterSeconds = resendAfterSeconds;
    }
}

