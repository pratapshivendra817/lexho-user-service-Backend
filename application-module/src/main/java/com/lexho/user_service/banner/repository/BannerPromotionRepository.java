package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BannerPromotionRepository extends JpaRepository<BannerPromotion, Long>, JpaSpecificationExecutor<BannerPromotion> {

    Optional<BannerPromotion> findByIdAndDeletedFalse(Long id);

    boolean existsByCouponCodeIgnoreCaseAndDeletedFalse(String couponCode);

    boolean existsByCouponCodeIgnoreCaseAndDeletedFalseAndIdNot(String couponCode, Long id);

    boolean existsByStatusAndPlacementAndSlotNumberAndDeletedFalse(BannerStatus status, BannerPlacement placement, Integer slotNumber);

    boolean existsByStatusAndPlacementAndSlotNumberAndDeletedFalseAndIdNot(BannerStatus status, BannerPlacement placement, Integer slotNumber, Long id);

    List<BannerPromotion> findByStatusAndEndDateBeforeAndDeletedFalse(BannerStatus status, LocalDateTime currentTime);

    List<BannerPromotion> findByStatusAndDeletedFalse(BannerStatus status);

    List<BannerPromotion> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<BannerPromotion> findByStatusAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanOrderByPriorityDescSlotNumberAsc(
            BannerStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
