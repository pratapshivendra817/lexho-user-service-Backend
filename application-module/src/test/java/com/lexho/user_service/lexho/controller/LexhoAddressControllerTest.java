package com.lexho.user_service.lexho.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.GlobalExceptionHandler;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import com.lexho.user_service.service.DeprecationControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LexhoAddressControllerTest {

    @Mock
    private LexhoAddressService addressService;
    @Mock
    private DeprecationControlService deprecationControlService;
    @Mock
    private DeprecatedApiUsageService deprecatedApiUsageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new LexhoAddressController(
                                addressService,
                                deprecationControlService,
                                deprecatedApiUsageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addReturnsAddressAndDeprecationHeaders() throws Exception {
        LexhoDtos.AddressResponse response =
                new LexhoDtos.AddressResponse(11L, "Home", "12 MG Road", "Bengaluru", true);
        when(addressService.addOrUpdate(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/address/add")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Home",
                                  "line1": "12 MG Road",
                                  "city": "Bengaluru",
                                  "latitude": 12.97,
                                  "longitude": 77.59
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Deprecated", "true"))
                .andExpect(header().string("X-API-Replacement", "/api/customer/location/**"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.isDefault").value(true));

        verify(deprecationControlService).ensureLegacyAddressModuleEnabled();
        verify(deprecatedApiUsageService).record("address.legacy.add");
    }

    @Test
    void listSupportsVersionedRouteAndReturnsAddresses() throws Exception {
        when(addressService.list(7L)).thenReturn(List.of(
                new LexhoDtos.AddressResponse(11L, "Home", "12 MG Road", "Bengaluru", true)));

        mockMvc.perform(get("/api/v1/address/list").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Deprecated", "true"))
                .andExpect(jsonPath("$.data[0].label").value("Home"));

        verify(deprecatedApiUsageService).record("address.legacy.list");
    }

    @Test
    void updateReturnsUpdatedAddress() throws Exception {
        when(addressService.addOrUpdate(any(), any())).thenReturn(
                new LexhoDtos.AddressResponse(11L, "Work", "Tech Park", "Bengaluru", false));

        mockMvc.perform(put("/api/address/update")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 11,
                                  "label": "Work",
                                  "line1": "Tech Park",
                                  "city": "Bengaluru"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("Work"))
                .andExpect(jsonPath("$.message").value("Address updated (deprecated route)"));

        verify(deprecatedApiUsageService).record("address.legacy.update");
    }

    @Test
    void deleteAndMakeDefaultDelegateToService() throws Exception {
        when(addressService.makeDefault(7L, 11L)).thenReturn(
                new LexhoDtos.AddressResponse(11L, "Home", "12 MG Road", "Bengaluru", true));

        mockMvc.perform(delete("/api/address/delete")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Address deleted (deprecated route)"));

        mockMvc.perform(put("/api/address/default")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        verify(addressService).delete(7L, 11L);
        verify(addressService).makeDefault(7L, 11L);
        verify(deprecatedApiUsageService).record("address.legacy.delete");
        verify(deprecatedApiUsageService).record("address.legacy.default");
    }

    @Test
    void invalidIdAndEmptyBodyReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/address/delete")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(put("/api/address/default")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void serviceFailureIsMappedByGlobalExceptionHandler() throws Exception {
        doThrow(new BadRequestException("Address coordinates are invalid"))
                .when(addressService).delete(7L, 99L);

        mockMvc.perform(delete("/api/address/delete")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LexhoDtos.IdRequest(99L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Address coordinates are invalid"));
    }
}
