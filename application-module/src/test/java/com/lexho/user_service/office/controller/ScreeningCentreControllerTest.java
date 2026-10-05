package com.lexho.user_service.office.controller;

import com.lexho.user_service.office.dto.ScreeningCentreAdminResponse;
import com.lexho.user_service.office.dto.ScreeningCentrePartnerResponse;
import com.lexho.user_service.office.dto.ScreeningCentreRequest;
import com.lexho.user_service.office.dto.ScreeningCentreStatusRequest;
import com.lexho.user_service.office.service.ScreeningCentreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreeningCentreControllerTest {

    private final ScreeningCentreService service = mock(ScreeningCentreService.class);
    private final AdminScreeningCentreController adminController = new AdminScreeningCentreController(service);
    private final PartnerScreeningCentreController partnerController = new PartnerScreeningCentreController(service);
    private MockMvc adminMockMvc;
    private MockMvc partnerMockMvc;

    @BeforeEach
    void setUp() {
        adminMockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        partnerMockMvc = MockMvcBuilders.standaloneSetup(partnerController).build();
    }

    @Test
    void adminGetPutAndStatusDelegateToService() {
        ScreeningCentreRequest request = request();
        ScreeningCentreAdminResponse response = adminResponse(true);
        when(service.getAdminScreeningCentre()).thenReturn(response);
        when(service.saveScreeningCentre(request, 99L)).thenReturn(response);
        when(service.updateStatus(false, 99L)).thenReturn(adminResponse(false));

        assertThat(adminController.get().getData().active()).isTrue();
        assertThat(adminController.save(99L, request).getData().latitude()).isEqualByComparingTo("28.5123456");
        assertThat(adminController.status(99L, new ScreeningCentreStatusRequest(false)).getData().active()).isFalse();

        verify(service).getAdminScreeningCentre();
        verify(service).saveScreeningCentre(request, 99L);
        verify(service).updateStatus(false, 99L);
    }

    @Test
    void partnerGetReturnsReadOnlyNavigationPayload() {
        ScreeningCentrePartnerResponse response = new ScreeningCentrePartnerResponse(
                "Lexho Screening Centre",
                "Gt-53, Noida Sector-117",
                new BigDecimal("28.5123456"),
                new BigDecimal("77.6123456"),
                "+919876543210");
        when(service.getPartnerScreeningCentre()).thenReturn(response);

        var result = partnerController.get();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().latitude()).isEqualByComparingTo("28.5123456");
        assertThat(result.getData().longitude()).isEqualByComparingTo("77.6123456");
        verify(service).getPartnerScreeningCentre();
    }

    @Test
    void httpPartnerGetReturnsExactNavigationCoordinatesWithoutAdminFields() throws Exception {
        ScreeningCentrePartnerResponse response = new ScreeningCentrePartnerResponse(
                "Lexho Screening Centre",
                "Gt-53, Noida Sector-117",
                new BigDecimal("28.5123456"),
                new BigDecimal("77.6123456"),
                "+919876543210");
        when(service.getPartnerScreeningCentre()).thenReturn(response);

        partnerMockMvc.perform(get("/api/v1/partner/screening-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lexho Screening Centre"))
                .andExpect(jsonPath("$.data.latitude").value(28.5123456))
                .andExpect(jsonPath("$.data.longitude").value(77.6123456))
                .andExpect(jsonPath("$.data.type").doesNotExist())
                .andExpect(jsonPath("$.data.active").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void httpAdminGetReturnsAdminMetadata() throws Exception {
        when(service.getAdminScreeningCentre()).thenReturn(adminResponse(true));

        adminMockMvc.perform(get("/api/v1/admin/screening-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("SCREENING_CENTRE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.latitude").value(28.5123456))
                .andExpect(jsonPath("$.data.longitude").value(77.6123456));
    }

    @Test
    void httpAdminPutRejectsInvalidLatitudeBeforeServiceCall() throws Exception {
        String payload = """
                {
                  "name": "Lexho Screening Centre",
                  "address": "Gt-53, Noida Sector-117",
                  "latitude": 91.0,
                  "longitude": 77.6123456,
                  "phone": "+919876543210"
                }
                """;

        adminMockMvc.perform(put("/api/v1/admin/screening-centre")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void controllersDeclareExpectedRoleRequirements() {
        assertThat(AdminScreeningCentreController.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('ADMIN')");
        assertThat(PartnerScreeningCentreController.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('PARTNER')");
    }

    private ScreeningCentreRequest request() {
        return new ScreeningCentreRequest(
                "Lexho Screening Centre",
                "Gt-53, Noida Sector-117",
                new BigDecimal("28.5123456"),
                new BigDecimal("77.6123456"),
                "+919876543210",
                null,
                null,
                null,
                null);
    }

    private ScreeningCentreAdminResponse adminResponse(boolean active) {
        return new ScreeningCentreAdminResponse(
                1L,
                "Lexho Screening Centre",
                "Gt-53, Noida Sector-117",
                new BigDecimal("28.5123456"),
                new BigDecimal("77.6123456"),
                "+919876543210",
                "SCREENING_CENTRE",
                active,
                null,
                null,
                null,
                null,
                OffsetDateTime.parse("2026-08-16T10:00:00Z"),
                OffsetDateTime.parse("2026-08-16T10:00:00Z"));
    }
}
