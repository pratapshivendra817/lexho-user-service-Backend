package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.PincodeAdjacencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PincodeAdjacencyRepository extends JpaRepository<PincodeAdjacencyEntity, Long> {
    List<PincodeAdjacencyEntity> findBySourcePincodeAndActiveTrueOrderByPriorityAsc(String sourcePincode);
}

