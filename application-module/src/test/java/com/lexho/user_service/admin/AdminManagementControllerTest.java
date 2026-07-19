package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminManagementController;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.admin.service.AdminFileUploadService;
import com.lexho.user_service.admin.service.AdminManagementService;
import com.lexho.user_service.admin.service.AdminSupportFaqService;
import com.lexho.user_service.customer.banner.service.AdminCustomerBannerService;
import com.lexho.user_service.config.JwtFilter;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.testconfig.WebSliceBootTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminManagementController.class)
class AdminManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminManagementService adminManagementService;
    @MockBean
    private AdminCatalogService adminCatalogService;
    @MockBean
    private AdminFileUploadService adminFileUploadService;
    @MockBean
    private AdminCustomerBannerService adminCustomerBannerService;
    @MockBean
    private AdminSupportFaqService adminSupportFaqService;
    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private ApiRateLimiterService apiRateLimiterService;

    @Test
    void uploadImageReturnsDataUrl() throws Exception {
        org.mockito.Mockito.when(adminFileUploadService.uploadServiceImage(any())).thenReturn("/uploads/services/abc.jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/upload/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://localhost/uploads/services/abc.jpg"));
    }

}
