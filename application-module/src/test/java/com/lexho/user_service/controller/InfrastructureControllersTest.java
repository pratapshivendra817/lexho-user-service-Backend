package com.lexho.user_service.controller;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.service.BookingManagementService;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfrastructureControllersTest {

    @Test
    void adminAndStatusControllersReturnStaticPayloads() {
        ApiResponse<Map<String, String>> dashboard = new AdminController().adminDashboard();
        ApiResponse<Map<String, Object>> status = new StatusController().getStatus();

        assertThat(dashboard.isSuccess()).isTrue();
        assertThat(dashboard.getData()).containsEntry("scope", "ADMIN");
        assertThat(status.isSuccess()).isTrue();
        assertThat(status.getData()).containsEntry("status", "UP");
        assertThat(status.getData().get("timestamp")).isNotNull();
    }

    @Test
    void opsControllerReturnsSummaryAndDeprecationSnapshot() {
        DeprecatedApiUsageService deprecatedApiUsageService = mock(DeprecatedApiUsageService.class);
        OpsController opsController = new OpsController(deprecatedApiUsageService);
        when(deprecatedApiUsageService.snapshot()).thenReturn(Map.of("count", 2, "items", List.of("legacy")));

        ApiResponse<Map<String, String>> summary = opsController.opsSummary();
        ApiResponse<Map<String, Object>> snapshot = opsController.deprecatedUsage();

        assertThat(summary.getData()).containsEntry("scope", "ADMIN_OR_PARTNER");
        assertThat(snapshot.getData()).containsEntry("count", 2);
        verify(deprecatedApiUsageService).snapshot();
    }

    @Test
    void internalBookingControllerSummarizesCompletedAndCancelledBookings() {
        BookingManagementService bookingManagementService = mock(BookingManagementService.class);
        InternalBookingController controller = new InternalBookingController(bookingManagementService);

        Booking completed = mock(Booking.class);
        Booking cancelled = mock(Booking.class);
        Booking active = mock(Booking.class);
        when(completed.getBookingStatus()).thenReturn(BookingStatus.COMPLETED);
        when(cancelled.getBookingStatus()).thenReturn(BookingStatus.CANCELLED);
        when(active.getBookingStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(bookingManagementService.getActiveBookingsForCustomer(55L)).thenReturn(List.of(completed, cancelled, active));

        BookingSummaryClientResponse response = controller.userBookingSummary(55L);

        assertThat(response.getTotalBookings()).isEqualTo(3);
        assertThat(response.getCompletedBookings()).isEqualTo(1);
        assertThat(response.getCancelledBookings()).isEqualTo(1);
    }

}
