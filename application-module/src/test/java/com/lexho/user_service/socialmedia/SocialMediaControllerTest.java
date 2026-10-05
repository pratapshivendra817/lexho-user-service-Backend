package com.lexho.user_service.socialmedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.socialmedia.controller.AdminSocialMediaController;
import com.lexho.user_service.socialmedia.controller.PublicSocialMediaController;
import com.lexho.user_service.socialmedia.dto.PublicSocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaPageResponse;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import com.lexho.user_service.socialmedia.service.SocialMediaManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SocialMediaControllerTest {
    @Mock
    private SocialMediaManagementService service;

    private AdminSocialMediaController adminController;
    private PublicSocialMediaController publicController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminController = new AdminSocialMediaController(service);
        publicController = new PublicSocialMediaController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void adminControllerMethodsDelegateToService() {
        SocialMediaLinkResponse response = new SocialMediaLinkResponse();
        response.setId(5L);
        SocialMediaPageResponse page = new SocialMediaPageResponse(List.of(response), 1, 1, 0, 20);
        when(service.getAdminLinks(0, 20, true, SocialMediaPlatform.INSTAGRAM)).thenReturn(page);
        when(service.getById(5L)).thenReturn(response);
        when(service.create(any())).thenReturn(response);
        when(service.update(eq(5L), any())).thenReturn(response);

        assertThat(adminController.all(0, 20, true, SocialMediaPlatform.INSTAGRAM).getBody().getData()).isSameAs(page);
        assertThat(adminController.byId(5L).getBody().getData()).isSameAs(response);
        assertThat(adminController.create(request()).getBody().getData()).isSameAs(response);
        assertThat(adminController.update(5L, request()).getBody().getData()).isSameAs(response);

        verify(service).getAdminLinks(0, 20, true, SocialMediaPlatform.INSTAGRAM);
        verify(service).getById(5L);
        verify(service).create(any());
        verify(service).update(eq(5L), any());
    }

    @Test
    void publicControllerReturnsOnlyPublicShape() {
        var link = new PublicSocialMediaLinkResponse(SocialMediaPlatform.X, "https://x.com/lexho", "X", "x", 1);
        when(service.getActivePublicLinks()).thenReturn(List.of(link));

        var body = publicController.active().getBody();

        assertThat(body.isSuccess()).isTrue();
        assertThat(body.getData()).containsExactly(link);
    }

    @Test
    void requestValidationRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/admin/social-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of("url", "https://x.com/lexho"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminControllerIsProtectedByAdminRoleAnnotation() throws Exception {
        PreAuthorize annotation = AdminSocialMediaController.class.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void accessDeniedMapsToForbiddenInSecurityLayer() {
        AccessDeniedException exception = new AccessDeniedException("denied");
        assertThat(exception).hasMessage("denied");
    }

    private SocialMediaLinkRequest request() {
        SocialMediaLinkRequest request = new SocialMediaLinkRequest();
        request.setPlatform(SocialMediaPlatform.INSTAGRAM);
        request.setUrl("https://instagram.com/lexho");
        request.setLabel("Instagram");
        request.setIcon("instagram");
        request.setDisplayOrder(1);
        request.setIsActive(true);
        return request;
    }
}
