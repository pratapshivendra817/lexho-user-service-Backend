package com.lexho.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.customer.location.controller.CustomerLocationController;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.service.AddressService;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.service.ApiRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerLocationTransitionIT {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LocationService locationService = Mockito.mock(LocationService.class);

    private final AddressService addressService = Mockito.mock(AddressService.class);

    private final ReverseGeocodeService reverseGeocodeService = Mockito.mock(ReverseGeocodeService.class);

    private final ServiceabilityService serviceabilityService = Mockito.mock(ServiceabilityService.class);
    private final ApiRateLimiterService apiRateLimiterService = Mockito.mock(ApiRateLimiterService.class);

    @BeforeEach
    void setUp() {
        CustomerLocationController controller = new CustomerLocationController(
                locationService, addressService, reverseGeocodeService, serviceabilityService, apiRateLimiterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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
        ServiceabilityCheckResponse response = new ServiceabilityCheckResponse();
        response.setServiceable(true);
        response.setMatchedPincode("452010");
        Mockito.when(serviceabilityService.resolve("452010", null, null)).thenReturn(response);

        mockMvc.perform(post("/api/customer/location/serviceability/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pincode\":\"452010\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceable").value(true));
    }
}
