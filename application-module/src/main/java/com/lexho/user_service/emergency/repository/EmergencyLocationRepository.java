package com.lexho.user_service.emergency.repository;

import com.lexho.user_service.emergency.entity.EmergencyLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyLocationRepository extends JpaRepository<EmergencyLocation, Long> {
}
