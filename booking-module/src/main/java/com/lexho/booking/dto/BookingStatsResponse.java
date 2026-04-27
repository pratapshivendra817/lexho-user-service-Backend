package com.lexho.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingStatsResponse {
    private long pending;
    private long confirmed;
    private long completed;
    private long cancelled;
}

