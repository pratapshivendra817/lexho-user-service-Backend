package com.lexho.user_service.banner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.service.AdminFileUploadService;
import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerExceptionHandler;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.service.AdminBannerPromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBannerPromotionControllerTest {

    private final AdminBannerPromotionService service = mock(AdminBannerPromotionService.class);
    private final AdminFileUploadService uploadService = mock(AdminFileUploadService.class);
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBannerPromotionController(service, uploadService))
                .setControllerAdvice(new BannerExceptionHandler())
                .build();
    }

    @Test
    void crudEndpointsShouldDelegateAndReturnPayloads() throws Exception {
        BannerPromotionResponse response = response(12L, BannerStatus.ACTIVE);
        when(service.create(any())).thenReturn(response);
        when(service.update(eq(12L), any())).thenReturn(response);
        when(service.getById(12L)).thenReturn(response);
        when(service.getAll(0, 10, "createdAt", "desc", null, null, null, null, null))
                .thenReturn(new BannerPageResponse<>(List.of(response), 1, 1, 0));
        when(service.activate(12L)).thenReturn(response);
        when(service.deactivate(12L)).thenReturn(response);
        when(service.moveToDraft(12L)).thenReturn(response);
        when(service.dashboardStats()).thenReturn(new BannerStatsResponse(1, 12.5, 100, 12, 2, new BigDecimal("44.00")));
        doNothing().when(service).softDelete(12L);

        mockMvc.perform(post("/api/admin/home-banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestJson())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/admin/home-banners/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestJson())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12));

        mockMvc.perform(get("/api/admin/home-banners/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));

        mockMvc.perform(get("/api/admin/home-banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12));

        mockMvc.perform(patch("/api/admin/home-banners/12/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/admin/home-banners/12/deactivate"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/home-banners/12/draft"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/home-banners/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeBanners").value(1))
                .andExpect(jsonPath("$.avgCtr").value(12.5));

        mockMvc.perform(delete("/api/admin/home-banners/12"))
                .andExpect(status().isNoContent());
        verify(service).softDelete(12L);
    }

    @Test
    void uploadAndExceptionPathsShouldReturnExpectedStatuses() throws Exception {
        when(uploadService.uploadBannerImage(any())).thenReturn("/uploads/banner.png");
        when(service.getById(99L)).thenThrow(new BannerNotFoundException(99L));
        when(service.create(any())).thenThrow(new BannerValidationException("Title is required"));

        MockMultipartFile file = new MockMultipartFile("file", "banner.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());
        mockMvc.perform(multipart("/api/admin/home-banners/upload/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists());

        mockMvc.perform(get("/api/admin/home-banners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Banner not found for id: 99"));

        mockMvc.perform(post("/api/admin/home-banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestJson())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));
    }

    private BannerPromotionResponse response(Long id, BannerStatus status) {
        BannerPromotionResponse response = new BannerPromotionResponse();
        response.setId(id);
        response.setTitle("Banner");
        response.setStatus(status);
        response.setPlacement(BannerPlacement.TOP);
        response.setPromoType(BannerPromoType.PROMO);
        response.setRedirectType(BannerRedirectType.NONE);
        response.setBannerImageUrl("/banner.png");
        return response;
    }

    private Object requestJson() {
        return new Object() {
            public final String title = "Promo";
            public final String bannerImageUrl = "/banner.png";
            public final String promoType = "PROMO";
            public final String status = "ACTIVE";
            public final String placement = "TOP";
            public final String redirectType = "NONE";
            public final int slotNumber = 1;
            public final int priority = 5;
            public final LocalDateTime startDate = LocalDateTime.of(2026, 6, 20, 10, 0);
            public final LocalDateTime endDate = LocalDateTime.of(2026, 6, 22, 10, 0);
            public final String platformType = "ALL";
        };
    }
}
