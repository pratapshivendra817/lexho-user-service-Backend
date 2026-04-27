package com.lexho.user_service.notification.dto;

import com.lexho.notification.enums.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChannelStatResponse {

    private ChannelType channelType;
    private long sent;
    private long failed;
}
