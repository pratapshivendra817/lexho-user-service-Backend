package com.lexho.user_service.notification.event;

import com.lexho.notification.enums.ChannelType;
import lombok.Getter;

@Getter
public class NotificationDispatchEvent {

    private final Long notificationId;
    private final ChannelType channelType;

    public NotificationDispatchEvent(Long notificationId, ChannelType channelType) {
        this.notificationId = notificationId;
        this.channelType = channelType;
    }
}
