package com.lexho.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "admin.auth")
public class AdminAuthProperties {

    private final Otp otp = new Otp();
    private final Password password = new Password();
    private final RateLimit rateLimit = new RateLimit();
    private final Infrastructure infrastructure = new Infrastructure();

    public Otp getOtp() {
        return otp;
    }

    public Password getPassword() {
        return password;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    public static class Otp {
        private String keyPrefix = "ADMIN_OTP_";
        private int expirySeconds = 300;
        private int maxAttempts = 3;
        private String deliveryFrom = "no-reply@lexho.com";

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public int getExpirySeconds() {
            return expirySeconds;
        }

        public void setExpirySeconds(int expirySeconds) {
            this.expirySeconds = expirySeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public String getDeliveryFrom() {
            return deliveryFrom;
        }

        public void setDeliveryFrom(String deliveryFrom) {
            this.deliveryFrom = deliveryFrom;
        }
    }

    public static class Password {
        private int maxFailedAttempts = 5;
        private int lockDurationMinutes = 30;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockDurationMinutes() {
            return lockDurationMinutes;
        }

        public void setLockDurationMinutes(int lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }
    }

    public static class RateLimit {
        private int maxRequests = 5;
        private int windowSeconds = 900;
        private String keyPrefix = "ADMIN_LOGIN_RL_";

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Infrastructure {
        private boolean allowInMemoryOtpFallback = false;
        private boolean allowInMemoryRateLimitFallback = false;
        private boolean allowConsoleOtpDeliveryFallback = false;

        public boolean isAllowInMemoryOtpFallback() {
            return allowInMemoryOtpFallback;
        }

        public void setAllowInMemoryOtpFallback(boolean allowInMemoryOtpFallback) {
            this.allowInMemoryOtpFallback = allowInMemoryOtpFallback;
        }

        public boolean isAllowInMemoryRateLimitFallback() {
            return allowInMemoryRateLimitFallback;
        }

        public void setAllowInMemoryRateLimitFallback(boolean allowInMemoryRateLimitFallback) {
            this.allowInMemoryRateLimitFallback = allowInMemoryRateLimitFallback;
        }

        public boolean isAllowConsoleOtpDeliveryFallback() {
            return allowConsoleOtpDeliveryFallback;
        }

        public void setAllowConsoleOtpDeliveryFallback(boolean allowConsoleOtpDeliveryFallback) {
            this.allowConsoleOtpDeliveryFallback = allowConsoleOtpDeliveryFallback;
        }
    }
}
