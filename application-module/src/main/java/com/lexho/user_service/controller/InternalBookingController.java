package com.lexho.user_service.controller;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.service.BookingManagementService;
import com.lexho.user.dto.BookingSummaryClientResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/bookings")
public class InternalBookingController {

    private final BookingManagementService bookingManagementService;

    public InternalBookingController(BookingManagementService bookingManagementService) {
        this.bookingManagementService = bookingManagementService;
    }

    @GetMapping("/users/{userId}/summary")
    public BookingSummaryClientResponse userBookingSummary(@PathVariable Long userId) {
        List<Booking> bookings = bookingManagementService.getActiveBookingsForCustomer(userId);
        long total = bookings.size();
        long completed = bookings.stream().filter(booking -> booking.getBookingStatus() == BookingStatus.COMPLETED).count();
        long cancelled = bookings.stream().filter(booking -> booking.getBookingStatus() == BookingStatus.CANCELLED).count();

        BookingSummaryClientResponse response = new BookingSummaryClientResponse();
        response.setTotalBookings(total);
        response.setCompletedBookings(completed);
        response.setCancelledBookings(cancelled);
        return response;
    }
}
