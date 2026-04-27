package com.lexho.user_service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.controller.AdminAuthController;
import com.lexho.user_service.dto.AdminLoginRequest;
import com.lexho.user_service.dto.AdminLoginResponse;
import com.lexho.user_service.dto.AdminTokenResponse;
import com.lexho.user_service.dto.AdminVerifyOtpRequest;
import com.lexho.user_service.service.AdminAuthService;
import com.lexho.user_service.TestWebSliceApp;
import com.lexho.user_service.config.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TestWebSliceApp.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminAuthController.class)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAuthService adminAuthService;
    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void loginReturnsOtpSentMessage() throws Exception {
        when(adminAuthService.login(any(AdminLoginRequest.class)))
                .thenReturn(new AdminLoginResponse("admin@lexho.com", 300));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@lexho.com");
        request.setPassword("Password@123");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    void verifyOtpReturnsAccessToken() throws Exception {
        when(adminAuthService.verifyOtp(any(AdminVerifyOtpRequest.class)))
                .thenReturn(new AdminTokenResponse("jwt-token"));

        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setEmail("admin@lexho.com");
        request.setOtp("123456");

        mockMvc.perform(post("/api/admin/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }
}
