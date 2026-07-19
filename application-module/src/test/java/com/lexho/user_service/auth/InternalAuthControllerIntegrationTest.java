package com.lexho.user_service.auth;

import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.controller.AuthTokenController;
import com.lexho.auth.service.AuthTokenService;
import com.lexho.common.enums.Role;
import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.config.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthTokenController.class)
class InternalAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthTokenService authTokenService;
    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void issueTokenShouldReturnTokenPair() throws Exception {
        when(authTokenService.issueTokens(11L, Role.ADMIN))
                .thenReturn(new TokenPairResponse("access-1", "refresh-1"));

        mockMvc.perform(post("/internal/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 11,
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-1"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"));
    }

    @Test
    void refreshTokenShouldReturnTokenPair() throws Exception {
        when(authTokenService.refreshTokens("refresh-old"))
                .thenReturn(new TokenPairResponse("access-2", "refresh-2"));

        mockMvc.perform(post("/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-old"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-2"));
    }

    @Test
    void revokeTokenShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/internal/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-to-revoke"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authTokenService).revokeRefreshToken(eq("refresh-to-revoke"));
    }
}
