package com.lexho.user.socket;

public record SignalEventPayload(
        Long sessionId,
        Long fromUserId,
        Long toUserId,
        Object data
) {
}
