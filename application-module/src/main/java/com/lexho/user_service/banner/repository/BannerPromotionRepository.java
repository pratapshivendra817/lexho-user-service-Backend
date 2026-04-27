package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BannerPromotionRepository extends JpaRepository<BannerPromotion, Long>, JpaSpecificationExecutor<BannerPromotion> {

    Optional<BannerPromotion> findByIdAndDeletedFalse(Long id);

    boolean existsByCouponCodeIgnoreCaseAndDeletedFalse(String couponCode);

    boolean existsByCouponCodeIgnoreCaseAndDeletedFalseAndIdNot(String couponCode, Long id);

    boolean existsByStatusAndSlotNumberAndDeletedFalse(BannerStatus status, Integer slotNumber);

    boolean existsByStatusAndSlotNumberAndDeletedFalseAndIdNot(BannerStatus status, Integer slotNumber, Long id);

    List<BannerPromotion> findByStatusAndEndDateBeforeAndDeletedFalse(BannerStatus status, LocalDateTime currentTime);
}

