package com.lexho.user_service.homebanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.GlobalExceptionHandler;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.homebanner.service.HomeBannerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeBannerControllersTest {

    private static final LocalValidatorFactoryBean VALIDATOR = validator();

    private final HomeBannerService homeBannerService = mock(HomeBannerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc adminMockMvc = MockMvcBuilders
            .standaloneSetup(new AdminHomeBannerController(homeBannerService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(VALIDATOR)
            .build();
    private final MockMvc customerMockMvc = MockMvcBuilders
            .standaloneSetup(new CustomerHomeBannerController(homeBannerService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(VALIDATOR)
            .build();

    @AfterAll
    static void closeValidator() {
        VALIDATOR.close();
    }

    @Test
    void adminCrudEndpointsAndCustomerListingReturnExpectedPayloads() throws Exception {
        HomeBannerResponse response = response(11L, "Banner");
        when(homeBannerService.create(any())).thenReturn(response);
        when(homeBannerService.update(eq(11L), any())).thenReturn(response);
        when(homeBannerService.getAdminBanners(HomeBannerAudience.CUSTOMER)).thenReturn(List.of(response));
        when(homeBannerService.reorder(any())).thenReturn(List.of(response));
        when(homeBannerService.getCustomerBanners()).thenReturn(List.of(response));

        adminMockMvc.perform(post("/api/admin/home/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Home banner created"))
                .andExpect(jsonPath("$.data.id").value(11));

        adminMockMvc.perform(put("/api/admin/home/banners/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Home banner updated"));

        adminMockMvc.perform(get("/api/admin/home/banners").param("audience", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Banner"));

        adminMockMvc.perform(patch("/api/admin/home/banners/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"id":11,"priority":2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Home banners reordered"));

        adminMockMvc.perform(delete("/api/admin/home/banners/11"))
                .andExpect(status().isNoContent());
        verify(homeBannerService).delete(11L);

        customerMockMvc.perform(get("/api/customer/home/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Home banners fetched"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://cdn.lexho.com/banner.png"));
    }

    @Test
    void adminEndpointsMapValidationAndBusinessErrors() throws Exception {
        adminMockMvc.perform(post("/api/admin/home/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","imageUrl":"","priority":-1,"active":true}
                                """))
                .andExpect(status().isBadRequest());

        adminMockMvc.perform(patch("/api/admin/home/banners/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());

        when(homeBannerService.update(eq(99L), any())).thenThrow(new ResourceNotFoundException("Home banner not found"));
        when(homeBannerService.create(any())).thenThrow(new BadRequestException("redirect_id is required"));

        adminMockMvc.perform(put("/api/admin/home/banners/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        adminMockMvc.perform(post("/api/admin/home/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("redirect_id is required"));
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }

    private static HomeBannerUpsertRequest validRequest() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest();
        request.setTitle("Banner");
        request.setSubtitle("Subtitle");
        request.setImageUrl("https://cdn.lexho.com/banner.png");
        request.setRedirectType(HomeBannerRedirectType.EXTERNAL);
        request.setPriority(2);
        request.setActive(true);
        request.setBannerType("promo");
        request.setAudience(HomeBannerAudience.CUSTOMER);
        return request;
    }

    private static HomeBannerResponse response(Long id, String title) {
        return HomeBannerResponse.builder()
                .id(id)
                .title(title)
                .subtitle("Subtitle")
                .imageUrl("https://cdn.lexho.com/banner.png")
                .redirectType(HomeBannerRedirectType.EXTERNAL)
                .priority(2)
                .active(true)
                .bannerType("promo")
                .audience(HomeBannerAudience.CUSTOMER)
                .createdAt(LocalDateTime.of(2026, 6, 19, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 19, 10, 0))
                .build();
    }
}
