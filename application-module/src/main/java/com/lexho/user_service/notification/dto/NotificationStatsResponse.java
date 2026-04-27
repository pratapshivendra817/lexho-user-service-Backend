package com.lexho.user_service.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NotificationStatsResponse {

    private long totalSent;
    private long totalFailed;
    private long totalRead;
    private long totalPending;
    private long totalClicked;
    private List<ChannelStatResponse> channelWiseStats;
}
