package com.lexho.user_service.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSendResponse {

    private int totalTargets;
    private int totalCreated;
    private String message;
}
