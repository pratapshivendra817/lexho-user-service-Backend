package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminBookingManagementController;
import com.lexho.user_service.admin.service.AdminBookingManagementService;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBookingManagementControllerTest {

    @Mock
    private AdminBookingManagementService adminBookingManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminBookingManagementController(adminBookingManagementService)).build();
    }

    @Test
    void navigationEndpointReturnsRouteEtaPayload() throws Exception {
        when(adminBookingManagementService.navigation(77L)).thenReturn(navigation(77L));

        mockMvc.perform(get("/api/v1/admin/booking-management/77/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Navigation ETA fetched"))
                .andExpect(jsonPath("$.data.bookingId").value(77))
                .andExpect(jsonPath("$.data.distanceKm").value(4.2))
                .andExpect(jsonPath("$.data.durationMinutes").value(12))
                .andExpect(jsonPath("$.data.routeRecalculated").value(true))
                .andExpect(jsonPath("$.data.liveTrackingEnabled").value(true));

        verify(adminBookingManagementService).navigation(77L);
    }

    @Test
    void liveTrackingEndpointReturnsNavigationBlock() throws Exception {
        when(adminBookingManagementService.liveTracking(55L)).thenReturn(Map.of(
                "bookingId", 55L,
                "partnerId", 99L,
                "navigation", navigation(55L),
                "events", java.util.List.of()
        ));

        mockMvc.perform(get("/api/v1/admin/booking-management/55/tracking/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(55))
                .andExpect(jsonPath("$.data.partnerId").value(99))
                .andExpect(jsonPath("$.data.navigation.bookingId").value(55))
                .andExpect(jsonPath("$.data.navigation.provider").value("MAPBOX"));

        verify(adminBookingManagementService).liveTracking(55L);
    }

    @Test
    void overridePartnerLocationReturnsUpdatedNavigation() throws Exception {
        when(adminBookingManagementService.overridePartnerLocation(eq(88L), eq(28.61), eq(77.20), eq("110001")))
                .thenReturn(Map.of(
                        "bookingId", 88L,
                        "partnerId", 42L,
                        "navigation", navigation(88L)
                ));

        mockMvc.perform(patch("/api/v1/admin/booking-management/88/tracking/partner-location")
                        .contentType("application/json")
                        .content("""
                                {
                                  "latitude": 28.61,
                                  "longitude": 77.20,
                                  "pincode": "110001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Partner location overridden"))
                .andExpect(jsonPath("$.data.partnerId").value(42))
                .andExpect(jsonPath("$.data.navigation.bookingId").value(88))
                .andExpect(jsonPath("$.data.navigation.routeRecalculated").value(true));

        verify(adminBookingManagementService).overridePartnerLocation(88L, 28.61, 77.20, "110001");
    }

    private NavigationRouteResponse navigation(Long bookingId) {
        return new NavigationRouteResponse(
                bookingId,
                4.2,
                12,
                "polyline",
                LocalDateTime.of(2026, 6, 14, 20, 45),
                true,
                "MAPBOX",
                false,
                true,
                false,
                null,
                2,
                true,
                28.61,
                77.20,
                LocalDateTime.of(2026, 6, 14, 20, 33),
                28.62,
                77.21,
                LocalDateTime.of(2026, 6, 14, 20, 34)
        );
    }
}
