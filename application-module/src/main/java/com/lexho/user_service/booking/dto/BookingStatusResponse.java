package com.lexho.user_service.booking.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class BookingStatusResponse {
    public Long id;
    public Long bookingId;
    public String bookingType;
    public String status;
    public String assignmentStatus;
    public Long serviceId;
    public String serviceName;
    public OffsetDateTime createdAt;
    public OffsetDateTime promisedStartTime;
    public OffsetDateTime scheduledAt;
    public LocalDate date;
    public String slot;
    public Integer slaMinutes;
    public Double totalAmount;
    public String fullAddress;
    public Integer etaMinutes;
    public Long partnerId;
    public String partnerName;
    public String partnerPhone;
    public Double partnerRating;
    public String partnerProfileImage;
    public String message;
}
