package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;

public interface LocationService {
    LocationUpdateResponse updateCurrentLocation(Long customerId, LocationUpdateRequest request);
    LocationUpdateResponse getCurrentLocation(Long customerId);
}
