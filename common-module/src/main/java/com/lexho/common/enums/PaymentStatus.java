package com.lexho.common.enums;

public enum PaymentStatus {

    PENDING,
    SUCCESS,
    FAILED;

    // 🔥 Helper methods (production level clarity)
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED;
    }
}
