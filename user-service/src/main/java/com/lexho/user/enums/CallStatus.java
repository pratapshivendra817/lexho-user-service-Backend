package com.lexho.user.enums;

public enum CallStatus {
    INITIATED,
    RINGING,
    CONNECTED,
    REJECTED,
    MISSED,
    BUSY,
    ENDED,
    FAILED;

    public boolean isActive() {
        return this == INITIATED || this == RINGING || this == CONNECTED;
    }
}
