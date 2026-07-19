package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.CookSubscriptionPricing;
import com.lexho.user_service.subscription.enums.FamilySize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CookSubscriptionPricingRepository extends JpaRepository<CookSubscriptionPricing, Long> {
    Optional<CookSubscriptionPricing> findByServiceIdAndFamilySizeAndActiveTrue(Long serviceId, FamilySize familySize);
    Optional<CookSubscriptionPricing> findByServiceIdIsNullAndFamilySizeAndActiveTrue(FamilySize familySize);
    List<CookSubscriptionPricing> findByServiceIdAndActiveTrueOrderByFamilySizeAsc(Long serviceId);
    List<CookSubscriptionPricing> findByServiceIdIsNullAndActiveTrueOrderByFamilySizeAsc();
    List<CookSubscriptionPricing> findByActiveTrueOrderByServiceIdAscFamilySizeAsc();
}
