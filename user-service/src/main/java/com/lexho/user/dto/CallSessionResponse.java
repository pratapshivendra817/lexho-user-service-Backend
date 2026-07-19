package com.lexho.user.dto;

import com.lexho.user.enums.CallStatus;

import java.time.LocalDateTime;

public record CallSessionResponse(
        Long id,
        Long callerId,
        Long receiverId,
        CallStatus status,
        LocalDateTime startTime,
        LocalDateTime answerTime,
        LocalDateTime endTime,
        Long duration,
        String stunUrl,
        String turnUrl,
        String turnUsername,
        String turnCredential
) {
}
