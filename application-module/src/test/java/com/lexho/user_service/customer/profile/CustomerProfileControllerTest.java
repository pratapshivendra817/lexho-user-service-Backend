package com.lexho.user_service.customer.profile;

import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.config.JwtFilter;
import com.lexho.user_service.customer.profile.controller.CustomerProfileController;
import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.customer.profile.service.CustomerProfileService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerProfileController.class)
class CustomerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerProfileService customerProfileService;
    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void uploadProfileImageAcceptsFileField() throws Exception {
        when(customerProfileService.uploadProfileImage(eq(101L), any())).thenReturn(new CustomerProfileResponse());
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());

        mockMvc.perform(multipart("/api/customer/profile/image").file(file).requestAttr("userId", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void uploadProfileImageAcceptsImageField() throws Exception {
        when(customerProfileService.uploadProfileImage(eq(202L), any())).thenReturn(new CustomerProfileResponse());
        MockMultipartFile image = new MockMultipartFile("image", "a.png", MediaType.IMAGE_PNG_VALUE, "img".getBytes());

        mockMvc.perform(multipart("/api/customer/profile/image").file(image).requestAttr("userId", 202L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void uploadProfileImageReturnsBadRequestWhenNoFileProvided() throws Exception {
        mockMvc.perform(multipart("/api/customer/profile/image").requestAttr("userId", 303L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image file is required"));

        verify(customerProfileService, never()).uploadProfileImage(any(), any());
    }
}
