package com.lexho.booking.dto;

import com.lexho.booking.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class BookingListItemResponse {
    private Long id;
    private String bookingId;
    private String customerName;
    private String providerName;
    private String serviceName;
    private BigDecimal amount;
    private LocalDate bookingDate;
    private BookingStatus status;
}

