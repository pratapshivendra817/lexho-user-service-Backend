package com.lexho.user_service.lexho.controller;

import com.lexho.user_service.exception.GlobalExceptionHandler;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.service.LexhoAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexhoAuthControllerTest {

    private final LexhoAuthService service = mock(LexhoAuthService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LexhoAuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sendsVerifiesRefreshesAndLogsOut() throws Exception {
        when(service.sendOtp("9999999999", null)).thenReturn("123456");
        when(service.getResendAfterSeconds()).thenReturn(30L);
        LexhoDtos.AuthVerifyResponse verifyResponse = new LexhoDtos.AuthVerifyResponse(
                "access", "refresh", new LexhoDtos.AuthUserDetails(1L, "Asha", "9999999999", null, null, "CUSTOMER"));
        when(service.verifyOtp("9999999999", "123456", "CUSTOMER")).thenReturn(verifyResponse);
        when(service.refreshToken("refresh")).thenReturn("new-access");

        mockMvc.perform(post("/otp/send").contentType("application/json")
                        .content("{\"mobile\":\"9999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mobile").value("9999999999"))
                .andExpect(jsonPath("$.data.otp").doesNotExist());
        mockMvc.perform(post("/otp/verify").contentType("application/json")
                        .content("{\"mobile\":\"9999999999\",\"otp\":\"123456\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("access"));
        mockMvc.perform(post("/auth/refresh-token").contentType("application/json")
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").value("new-access"));
        mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"refresh\",\"deviceId\":\"d1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").value("new-access"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"refresh\",\"deviceId\":\"d1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.token").value("new-access"));
        mockMvc.perform(post("/auth/logout").contentType("application/json")
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk());
        verify(service).logout("refresh");
    }

    @Test
    void rejectsInvalidOtpPayload() throws Exception {
        mockMvc.perform(post("/otp/verify").contentType("application/json")
                        .content("{\"mobile\":\"123\",\"otp\":\"1\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }
}
