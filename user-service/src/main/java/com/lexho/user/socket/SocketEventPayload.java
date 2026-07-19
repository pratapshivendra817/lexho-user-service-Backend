package com.lexho.user.socket;

import com.lexho.user.enums.CallStatus;

public record SocketEventPayload(
        Long sessionId,
        Long callerId,
        Long receiverId,
        CallStatus status,
        Long duration,
        String stunUrl,
        String turnUrl,
        String turnUsername,
        String turnCredential,
        Object data
) {
}
