package com.lexho.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.service.AuthTokenService;
import com.lexho.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTokenControllerTest {

    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthTokenController(authTokenService))
                .setControllerAdvice(new AuthExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void issueShouldReturnTokenPair() throws Exception {
        when(authTokenService.issueTokens(10L, Role.ADMIN)).thenReturn(new TokenPairResponse("access", "refresh"));

        mockMvc.perform(post("/internal/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":10,\"role\":\" admin \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        verify(authTokenService).issueTokens(10L, Role.ADMIN);
    }

    @Test
    void issueShouldRejectInvalidRoleAndValidationFailures() throws Exception {
        mockMvc.perform(post("/internal/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":10,\"role\":\"bad-role\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid role"));

        mockMvc.perform(post("/internal/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId must not be null"));
    }

    @Test
    void refreshShouldReturnTokenPairAndMapServiceErrors() throws Exception {
        when(authTokenService.refreshTokens("refresh-token")).thenReturn(new TokenPairResponse("new-access", "new-refresh"));
        when(authTokenService.refreshTokens("invalid")).thenThrow(new IllegalArgumentException("Invalid refresh token"));

        mockMvc.perform(post("/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" refresh-token \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));

        mockMvc.perform(post("/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        mockMvc.perform(post("/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("refreshToken must not be blank"));
    }

    @Test
    void revokeShouldReturnNoContent() throws Exception {
        doNothing().when(authTokenService).revokeRefreshToken(eq("refresh-token"));

        mockMvc.perform(post("/internal/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" refresh-token \"}"))
                .andExpect(status().isNoContent());

        verify(authTokenService).revokeRefreshToken("refresh-token");
    }
}
