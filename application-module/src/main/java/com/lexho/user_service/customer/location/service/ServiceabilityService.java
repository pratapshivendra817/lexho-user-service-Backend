package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;

public interface ServiceabilityService {
    boolean isServiceable(String pincode);
    ServiceabilityCheckResponse resolve(String pincode, Double latitude, Double longitude);
    ServiceabilityCheckResponse resolveForCustomer(Long customerId);
}
