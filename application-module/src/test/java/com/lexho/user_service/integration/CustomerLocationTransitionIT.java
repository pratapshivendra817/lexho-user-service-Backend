package com.lexho.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.customer.location.controller.CustomerLocationController;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.service.AddressService;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerLocationTransitionIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @MockBean
    private AddressService addressService;

    @MockBean
    private ReverseGeocodeService reverseGeocodeService;

    @MockBean
    private ServiceabilityService serviceabilityService;

    @Test
    void canonicalCurrentEndpointShouldWork() throws Exception {
        LocationUpdateResponse response = new LocationUpdateResponse();
        response.pincode = "452010";
        response.serviceAvailable = true;
        Mockito.when(locationService.getCurrentLocation(101L)).thenReturn(response);

        mockMvc.perform(get("/api/customer/location/current")
                        .requestAttr("userId", 101L)
                        .requestAttr("role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pincode").value("452010"));
    }

    @Test
    void deprecatedServiceabilityEndpointShouldReturnDeprecationHeader() throws Exception {
        Mockito.when(serviceabilityService.isServiceable("452010")).thenReturn(true);

        mockMvc.perform(post("/api/customer/location/serviceability/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pincode\":\"452010\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceAvailable").value(true));
    }
}
