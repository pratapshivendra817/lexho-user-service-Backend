package com.lexho.user_service.customer.location.service.impl;

import com.lexho.user_service.customer.location.repository.ServiceablePincodeRepository;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import org.springframework.stereotype.Service;

@Service
public class ServiceabilityServiceImpl implements ServiceabilityService {
    private final ServiceablePincodeRepository repository;

    public ServiceabilityServiceImpl(ServiceablePincodeRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isServiceable(String pincode) {
        return repository.existsByPincodeAndActiveTrue(pincode == null ? "" : pincode.trim());
    }
}
