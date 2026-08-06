package com.lexho.user_service.partnerrequest.controller;

import com.lexho.user_service.config.SecurityExposureProperties;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.GlobalExceptionHandler;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.service.PartnerRequestService;
import com.lexho.user_service.service.ApiRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartnerRequestControllerTest {

    @Mock
    private PartnerRequestService partnerRequestService;
    @Mock
    private ApiRateLimiterService apiRateLimiterService;
    @Mock
    private SecurityExposureProperties securityExposureProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PartnerRequestController controller = new PartnerRequestController(
                partnerRequestService,
                apiRateLimiterService,
                securityExposureProperties
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void acceptShouldReturnSuccessWhenRequestAccepted() throws Exception {
        PartnerRequestActionResponse response = new PartnerRequestActionResponse();
        response.setRequestId(101L);
        response.setStatus(PartnerServiceRequestStatus.ACCEPTED);

        doNothing().when(apiRateLimiterService).check(anyString(), anyString(), anyInt(), any(), anyString());
        when(partnerRequestService.accept(7L, 101L)).thenReturn(response);

        mockMvc.perform(patch("/api/partner/requests/101/accept")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value(101))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void acceptShouldReturnBadRequestWhenAlreadyAcceptedByAnotherPartner() throws Exception {
        doNothing().when(apiRateLimiterService).check(anyString(), anyString(), anyInt(), any(), anyString());
        when(partnerRequestService.accept(8L, 101L))
                .thenThrow(new BadRequestException("request is not pending"));

        mockMvc.perform(patch("/api/partner/requests/101/accept")
                        .requestAttr("userId", 8L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("request is not pending"));
    }
}
