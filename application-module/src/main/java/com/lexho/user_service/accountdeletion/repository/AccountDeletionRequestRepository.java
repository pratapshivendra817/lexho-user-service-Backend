package com.lexho.user_service.accountdeletion.repository;

import com.lexho.user_service.accountdeletion.entity.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {
    Optional<AccountDeletionRequest> findTopByUserIdAndUserTypeOrderByRequestedAtDesc(Long userId, String userType);
    boolean existsByUserIdAndUserTypeAndStatusIn(Long userId, String userType, List<String> statuses);
    List<AccountDeletionRequest> findByStatusOrderByRequestedAtDesc(String status);
}
