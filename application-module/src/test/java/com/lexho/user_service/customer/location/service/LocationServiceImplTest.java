package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LocationServiceImplTest {

    @Test
    void updateCurrentLocation_shouldResolveServiceability() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.empty());
        when(locationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.pincode = "201304";
        geo.city = "Noida";
        geo.state = "UP";
        when(reverse.reverse(28.6123, 77.2290)).thenReturn(geo);
        when(serviceability.isServiceable("201304")).thenReturn(true);

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, serviceability);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 28.6123;
        request.longitude = 77.2290;

        LocationUpdateResponse response = service.updateCurrentLocation(99L, request);
        assertTrue(response.serviceAvailable);
        assertEquals("201304", response.pincode);
    }
}
