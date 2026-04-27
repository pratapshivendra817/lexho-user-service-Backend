package com.lexho.booking.dto;

import com.lexho.booking.enums.BookingStatus;
import com.lexho.common.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class BookingDetailResponse {
    private Long id;
    private String bookingId;
    private Long customerId;
    private String customerName;
    private Long providerId;
    private String providerName;
    private Long serviceId;
    private String serviceName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private BigDecimal amount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String address;
    private String city;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

