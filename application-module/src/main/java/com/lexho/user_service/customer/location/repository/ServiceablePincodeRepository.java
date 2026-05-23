package com.lexho.user_service.customer.location.repository;

import com.lexho.user_service.customer.location.entity.ServiceablePincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, String> {
    Optional<ServiceablePincode> findByPincodeAndActiveTrue(String pincode);
    boolean existsByPincodeAndActiveTrue(String pincode);
}
