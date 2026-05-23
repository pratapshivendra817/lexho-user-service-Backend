package com.lexho.user_service.customer.location.service.impl;

import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.exception.InvalidCoordinatesException;
import com.lexho.user_service.customer.location.exception.AddressNotFoundException;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LocationServiceImpl implements LocationService {
    private final CustomerCurrentLocationRepository repository;
    private final ReverseGeocodeService reverseGeocodeService;
    private final ServiceabilityService serviceabilityService;

    public LocationServiceImpl(CustomerCurrentLocationRepository repository,
                               ReverseGeocodeService reverseGeocodeService,
                               ServiceabilityService serviceabilityService) {
        this.repository = repository;
        this.reverseGeocodeService = reverseGeocodeService;
        this.serviceabilityService = serviceabilityService;
    }

    @Override
    @Transactional
    public LocationUpdateResponse updateCurrentLocation(Long customerId, LocationUpdateRequest request) {
        validateCoordinates(request.latitude, request.longitude);
        ReverseGeocodeResponse reverse = reverseGeocodeService.reverse(request.latitude, request.longitude);
        String pincode = reverse.pincode == null ? "" : reverse.pincode.trim();
        boolean serviceable = !pincode.isBlank() && serviceabilityService.isServiceable(pincode);

        CustomerCurrentLocation location = repository.findByCustomerId(customerId).orElseGet(CustomerCurrentLocation::new);
        location.setCustomerId(customerId);
        location.setLatitude(BigDecimal.valueOf(request.latitude));
        location.setLongitude(BigDecimal.valueOf(request.longitude));
        location.setFormattedAddress(reverse.formattedAddress);
        location.setCity(reverse.city);
        location.setState(reverse.state);
        location.setPincode(pincode);
        location.setServiceAvailable(serviceable);
        repository.save(location);

        LocationUpdateResponse response = new LocationUpdateResponse();
        response.serviceAvailable = serviceable;
        response.pincode = pincode;
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LocationUpdateResponse getCurrentLocation(Long customerId) {
        CustomerCurrentLocation location = repository.findByCustomerId(customerId)
                .orElseThrow(() -> new AddressNotFoundException("Current location not found"));
        LocationUpdateResponse response = new LocationUpdateResponse();
        response.pincode = location.getPincode();
        response.serviceAvailable = location.isServiceAvailable();
        return response;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidCoordinatesException("Invalid latitude/longitude");
        }
    }
}
