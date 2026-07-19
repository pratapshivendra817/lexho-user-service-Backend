package com.lexho.user_service.customer.location.controller;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.PagedResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeRequest;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckRequest;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.dto.UpdateAddressRequest;
import com.lexho.user_service.customer.location.service.AddressService;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.service.ApiRateLimiterService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerLocationControllerTest {

    @Test
    void customerLocationControllerDelegatesAllEndpointsAndUsesFallbackSearch() {
        LocationService locationService = mock(LocationService.class);
        AddressService addressService = mock(AddressService.class);
        ReverseGeocodeService reverseGeocodeService = mock(ReverseGeocodeService.class);
        ServiceabilityService serviceabilityService = mock(ServiceabilityService.class);
        ApiRateLimiterService apiRateLimiterService = mock(ApiRateLimiterService.class);
        CustomerLocationController controller = new CustomerLocationController(
                locationService, addressService, reverseGeocodeService, serviceabilityService, apiRateLimiterService);

        LocationUpdateRequest updateRequest = new LocationUpdateRequest();
        updateRequest.latitude = 12.3;
        updateRequest.longitude = 45.6;
        LocationUpdateResponse updateResponse = new LocationUpdateResponse();
        updateResponse.pincode = "110001";
        updateResponse.serviceAvailable = true;
        when(locationService.updateCurrentLocation(7L, updateRequest)).thenReturn(updateResponse);
        when(locationService.getCurrentLocation(7L)).thenReturn(updateResponse);

        SaveAddressRequest saveAddressRequest = new SaveAddressRequest();
        UpdateAddressRequest updateAddressRequest = new UpdateAddressRequest();
        AddressResponse addressResponse = new AddressResponse();
        addressResponse.city = "Delhi";
        PagedResponse<AddressResponse> pagedResponse = new PagedResponse<>();
        pagedResponse.items = java.util.List.of(addressResponse);
        UUID addressId = UUID.randomUUID();
        when(addressService.save(7L, saveAddressRequest)).thenReturn(addressResponse);
        when(addressService.update(7L, addressId, updateAddressRequest)).thenReturn(addressResponse);
        when(addressService.list(7L, 0, 10, "term")).thenReturn(pagedResponse);
        when(addressService.list(7L, 1, 20, "queryOnly")).thenReturn(pagedResponse);
        when(addressService.setDefault(7L, addressId)).thenReturn(addressResponse);

        ReverseGeocodeRequest reverseRequest = new ReverseGeocodeRequest();
        reverseRequest.latitude = 12.3;
        reverseRequest.longitude = 45.6;
        ReverseGeocodeResponse reverseResponse = new ReverseGeocodeResponse();
        reverseResponse.city = "Noida";
        when(reverseGeocodeService.reverse(12.3, 45.6)).thenReturn(reverseResponse);

        ServiceabilityCheckRequest serviceabilityCheckRequest = new ServiceabilityCheckRequest();
        serviceabilityCheckRequest.latitude = 12.3;
        serviceabilityCheckRequest.longitude = 45.6;
        serviceabilityCheckRequest.pincode = "110001";
        ServiceabilityCheckResponse serviceabilityCheckResponse = new ServiceabilityCheckResponse();
        serviceabilityCheckResponse.setServiceable(true);
        when(serviceabilityService.resolve("110001", 12.3, 45.6)).thenReturn(serviceabilityCheckResponse);

        assertThat(controller.update(7L, "CUSTOMER", updateRequest).getBody().getData()).isSameAs(updateResponse);
        assertThat(controller.saveAddress(7L, "CUSTOMER", saveAddressRequest).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.updateAddress(7L, "CUSTOMER", addressId, updateAddressRequest).getBody().getData()).isSameAs(addressResponse);
        assertThat(controller.current(7L, "CUSTOMER").getBody().getData()).isSameAs(updateResponse);
        assertThat(controller.all(7L, "CUSTOMER", 0, 10, "term", "ignored").getBody().getData()).isSameAs(pagedResponse);
        assertThat(controller.all(7L, "CUSTOMER", 1, 20, null, "queryOnly").getBody().getData()).isSameAs(pagedResponse);
        assertThat(controller.search(7L, "CUSTOMER", "queryOnly", 1, 20).getBody().getData()).isSameAs(pagedResponse);
        assertThat(controller.setDefault(7L, "CUSTOMER", addressId).getBody().getData()).isSameAs(addressResponse);
        assertThat(controller.delete(7L, "CUSTOMER", addressId).getBody().isSuccess()).isTrue();
        assertThat(controller.reverseGeocode(reverseRequest).getBody().getData()).isSameAs(reverseResponse);
        assertThat(controller.checkServiceability(serviceabilityCheckRequest).getBody().getData()).isSameAs(serviceabilityCheckResponse);

        verify(apiRateLimiterService).check(eq("location_update"), eq("7"), eq(120), any(), eq("Location update rate limit exceeded"));
        verify(apiRateLimiterService).check(eq("location_reverse_geocode"), eq("global"), eq(300), any(), eq("Reverse geocode rate limit exceeded"));
        verify(apiRateLimiterService).check(eq("location_serviceability_check"), eq("global"), eq(300), any(), eq("Serviceability check rate limit exceeded"));
        verify(addressService).softDelete(7L, addressId);
    }

    @Test
    void customerLocationControllerRejectsNonCustomerRole() {
        CustomerLocationController controller = new CustomerLocationController(
                mock(LocationService.class),
                mock(AddressService.class),
                mock(ReverseGeocodeService.class),
                mock(ServiceabilityService.class),
                mock(ApiRateLimiterService.class));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> controller.current(7L, "ADMIN"));

        assertThat(exception).hasMessage("Unauthorized access");
    }
}
