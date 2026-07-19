package com.lexho.provider.repository;

import com.lexho.provider.entity.Provider;
import com.lexho.provider.enums.OnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long>, JpaSpecificationExecutor<Provider> {

    Optional<Provider> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

    long countByOnboardingStatusAndDeletedFalse(OnboardingStatus onboardingStatus);
}
