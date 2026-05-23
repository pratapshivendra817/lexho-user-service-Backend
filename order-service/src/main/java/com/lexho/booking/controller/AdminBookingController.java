package com.lexho.booking.controller;

import com.lexho.booking.dto.BookingDetailResponse;
import com.lexho.booking.dto.BookingListItemResponse;
import com.lexho.booking.dto.BookingStatsResponse;
import com.lexho.booking.dto.BookingUpdateRequest;
import com.lexho.booking.dto.MessageResponse;
import com.lexho.booking.dto.PageResponse;
import com.lexho.booking.service.BookingManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Booking Management", description = "Admin APIs for booking management")
public class AdminBookingController {

    private final BookingManagementService bookingManagementService;

    public AdminBookingController(BookingManagementService bookingManagementService) {
        this.bookingManagementService = bookingManagementService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get booking dashboard stats")
    public ResponseEntity<BookingStatsResponse> stats() {
        return ResponseEntity.ok(bookingManagementService.getStats());
    }

    @GetMapping
    @Operation(summary = "Get all bookings with pagination, sorting and filters")
    public ResponseEntity<PageResponse<BookingListItemResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType) {
        return ResponseEntity.ok(bookingManagementService.getAllBookings(
                page, size, sortBy, sortDir, search, status, serviceType
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by id")
    public ResponseEntity<BookingDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingManagementService.getBookingById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update booking details")
    public ResponseEntity<BookingDetailResponse> updateBooking(@PathVariable Long id,
                                                               @Valid @RequestBody BookingUpdateRequest request) {
        return ResponseEntity.ok(bookingManagementService.updateBooking(id, request));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<BookingDetailResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingManagementService.cancelBooking(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete booking")
    public ResponseEntity<MessageResponse> softDelete(@PathVariable Long id) {
        bookingManagementService.softDeleteBooking(id);
        return ResponseEntity.ok(new MessageResponse("Booking deleted successfully"));
    }
}
