package com.lexho.user_service.customer.location.repository;

import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerCurrentLocationRepository extends JpaRepository<CustomerCurrentLocation, UUID> {
    Optional<CustomerCurrentLocation> findByCustomerId(Long customerId);
}
