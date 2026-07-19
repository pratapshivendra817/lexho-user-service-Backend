package com.lexho.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.dto.BookingDetailResponse;
import com.lexho.booking.dto.BookingListItemResponse;
import com.lexho.booking.dto.BookingStatsResponse;
import com.lexho.booking.dto.BookingUpdateRequest;
import com.lexho.booking.dto.PageResponse;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.exception.BookingExceptionHandler;
import com.lexho.booking.exception.BookingNotFoundException;
import com.lexho.booking.exception.BookingOperationException;
import com.lexho.booking.service.BookingManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    @Mock
    private BookingManagementService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBookingController(service))
                .setControllerAdvice(new BookingExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void statsReturnsDashboardCounts() throws Exception {
        when(service.getStats()).thenReturn(new BookingStatsResponse(1, 2, 3, 4));

        mockMvc.perform(get("/api/v1/admin/bookings/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.confirmed").value(2))
                .andExpect(jsonPath("$.completed").value(3))
                .andExpect(jsonPath("$.cancelled").value(4));
    }

    @Test
    void listForwardsAllQueryParametersAndSupportsLegacyAlias() throws Exception {
        BookingListItemResponse item = new BookingListItemResponse();
        item.setId(9L);
        item.setBookingId("BK9");
        item.setStatus(BookingStatus.CONFIRMED);
        when(service.getAllBookings(2, 25, "amount", "asc", "sam", "CONFIRMED", "Cleaning"))
                .thenReturn(new PageResponse<>(List.of(item), 1, 2, 25));

        mockMvc.perform(get("/api/admin/bookings")
                        .param("page", "2")
                        .param("size", "25")
                        .param("sortBy", "amount")
                        .param("sortDir", "asc")
                        .param("search", "sam")
                        .param("status", "CONFIRMED")
                        .param("serviceType", "Cleaning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.content[0].bookingId").value("BK9"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void listUsesControllerDefaults() throws Exception {
        when(service.getAllBookings(0, 10, "createdAt", "desc", null, null, null))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 10));

        mockMvc.perform(get("/api/v1/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getByIdReturnsDetail() throws Exception {
        BookingDetailResponse response = detail(7L);
        when(service.getBookingById(7L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/bookings/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.bookingId").value("BK7"));
    }

    @Test
    void getByIdMapsNotFoundException() throws Exception {
        when(service.getBookingById(404L)).thenThrow(new BookingNotFoundException(404L));

        mockMvc.perform(get("/api/v1/admin/bookings/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found for id: 404"));
    }

    @Test
    void updateAcceptsValidRequestAndReturnsDetail() throws Exception {
        BookingUpdateRequest request = validRequest();
        when(service.updateBooking(any(Long.class), any(BookingUpdateRequest.class))).thenReturn(detail(5L));

        mockMvc.perform(put("/api/v1/admin/bookings/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(service).updateBooking(any(Long.class), any(BookingUpdateRequest.class));
    }

    @Test
    void updateRejectsMissingRequiredField() throws Exception {
        BookingUpdateRequest request = validRequest();
        request.setProviderId(null);

        mockMvc.perform(put("/api/v1/admin/bookings/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Partner id is required"));
    }

    @Test
    void updateRejectsNonPositiveAmountAndOversizedNotes() throws Exception {
        BookingUpdateRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setNotes("x".repeat(1001));

        mockMvc.perform(put("/api/v1/admin/bookings/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMapsOperationFailureToBadRequest() throws Exception {
        when(service.updateBooking(any(Long.class), any(BookingUpdateRequest.class)))
                .thenThrow(new BookingOperationException("Completed bookings cannot be edited"));

        mockMvc.perform(put("/api/v1/admin/bookings/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completed bookings cannot be edited"));
    }

    @Test
    void cancelReturnsUpdatedBooking() throws Exception {
        BookingDetailResponse response = detail(6L);
        response.setBookingStatus(BookingStatus.CANCELLED);
        when(service.cancelBooking(6L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/bookings/6/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }

    @Test
    void deleteReturnsConfirmationMessage() throws Exception {
        doNothing().when(service).softDeleteBooking(8L);

        mockMvc.perform(delete("/api/v1/admin/bookings/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking deleted successfully"));

        verify(service).softDeleteBooking(8L);
    }

    private BookingUpdateRequest validRequest() {
        BookingUpdateRequest request = new BookingUpdateRequest();
        request.setBookingDate(LocalDate.of(2026, 7, 1));
        request.setBookingTime(LocalTime.of(10, 30));
        request.setProviderId(11L);
        request.setAmount(BigDecimal.valueOf(500));
        request.setNotes("Doorbell is broken");
        return request;
    }

    private BookingDetailResponse detail(Long id) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(id);
        response.setBookingId("BK" + id);
        response.setBookingStatus(BookingStatus.PENDING);
        return response;
    }
}
