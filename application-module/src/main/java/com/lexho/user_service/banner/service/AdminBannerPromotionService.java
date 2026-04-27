package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminBannerPromotionService {

    private final BannerPromotionRepository bannerPromotionRepository;
    private final BannerPromotionMapper bannerPromotionMapper;
    private final SecurityUtils securityUtils;

    public AdminBannerPromotionService(BannerPromotionRepository bannerPromotionRepository,
                                       BannerPromotionMapper bannerPromotionMapper,
                                       SecurityUtils securityUtils) {
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.bannerPromotionMapper = bannerPromotionMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse create(BannerUpsertRequest request) {
        validateRequest(request, null);
        BannerPromotion banner = new BannerPromotion();
        bannerPromotionMapper.apply(banner, request);
        banner.setDeleted(false);
        banner.setCreatedBy(currentUserId());
        banner.setUpdatedBy(currentUserId());
        BannerPromotion saved = bannerPromotionRepository.save(banner);
        return bannerPromotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse update(Long id, BannerUpsertRequest request) {
        BannerPromotion banner = getBanner(id);
        validateRequest(request, id);
        bannerPromotionMapper.apply(banner, request);
        banner.setUpdatedBy(currentUserId());
        BannerPromotion saved = bannerPromotionRepository.save(banner);
        return bannerPromotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public void softDelete(Long id) {
        BannerPromotion banner = getBanner(id);
        banner.setDeleted(true);
        banner.setUpdatedBy(currentUserId());
        bannerPromotionRepository.save(banner);
    }

    @Transactional(readOnly = true)
    public BannerPromotionResponse getById(Long id) {
        return bannerPromotionMapper.toResponse(getBanner(id));
    }

    @Transactional(readOnly = true)
    public BannerPageResponse<BannerPromotionResponse> getAll(int page,
                                                              int size,
                                                              String sortBy,
                                                              String sortDir,
                                                              BannerStatus status,
                                                              String city,
                                                              String promoType,
                                                              String platformType,
                                                              String search) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, normalizeSortBy(sortBy))
        );

        Specification<BannerPromotion> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            }
            if (promoType != null && !promoType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("promoType")), promoType.toUpperCase()));
            }
            if (platformType != null && !platformType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("platformType")), platformType.toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("subtitle")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("couponCode")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BannerPromotion> result = bannerPromotionRepository.findAll(specification, pageable);
        List<BannerPromotionResponse> content = result.getContent().stream().map(bannerPromotionMapper::toResponse).toList();
        return new BannerPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse activate(Long id) {
        BannerPromotion banner = getBanner(id);
        if (banner.getStatus() == BannerStatus.EXPIRED) {
            throw new BannerValidationException("Expired banner cannot be activated");
        }
        if (bannerPromotionRepository.existsByStatusAndSlotNumberAndDeletedFalseAndIdNot(BannerStatus.ACTIVE, banner.getSlotNumber(), id)) {
            throw new BannerValidationException("Active banner slot already in use: " + banner.getSlotNumber());
        }
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setUpdatedBy(currentUserId());
        return bannerPromotionMapper.toResponse(bannerPromotionRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse deactivate(Long id) {
        BannerPromotion banner = getBanner(id);
        banner.setStatus(BannerStatus.INACTIVE);
        banner.setUpdatedBy(currentUserId());
        return bannerPromotionMapper.toResponse(bannerPromotionRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse moveToDraft(Long id) {
        BannerPromotion banner = getBanner(id);
        banner.setStatus(BannerStatus.DRAFT);
        banner.setUpdatedBy(currentUserId());
        return bannerPromotionMapper.toResponse(bannerPromotionRepository.save(banner));
    }

    @Transactional(readOnly = true)
    public BannerStatsResponse dashboardStats() {
        List<BannerPromotion> banners = bannerPromotionRepository.findAll();
        long active = banners.stream().filter(b -> !b.isDeleted() && b.getStatus() == BannerStatus.ACTIVE).count();
        long impressions = banners.stream().filter(b -> !b.isDeleted()).mapToLong(BannerPromotion::getImpressionCount).sum();
        long clicks = banners.stream().filter(b -> !b.isDeleted()).mapToLong(BannerPromotion::getClickCount).sum();
        long redemptions = banners.stream().filter(b -> !b.isDeleted()).mapToLong(BannerPromotion::getRedemptionCount).sum();
        BigDecimal revenue = banners.stream()
                .filter(b -> !b.isDeleted())
                .map(BannerPromotion::getRevenueGenerated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double ctr = impressions == 0 ? 0.0 : ((double) clicks / impressions) * 100.0;
        double roundedCtr = BigDecimal.valueOf(ctr).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return new BannerStatsResponse(active, roundedCtr, impressions, clicks, redemptions, revenue);
    }

    private BannerPromotion getBanner(Long id) {
        return bannerPromotionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
    }

    private void validateRequest(BannerUpsertRequest request, Long id) {
        if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
            throw new BannerValidationException("End date must be greater than start date");
        }
        if (request.getStatus() == BannerStatus.ACTIVE) {
            boolean slotExists = id == null
                    ? bannerPromotionRepository.existsByStatusAndSlotNumberAndDeletedFalse(BannerStatus.ACTIVE, request.getSlotNumber())
                    : bannerPromotionRepository.existsByStatusAndSlotNumberAndDeletedFalseAndIdNot(BannerStatus.ACTIVE, request.getSlotNumber(), id);
            if (slotExists) {
                throw new BannerValidationException("Active banner slot already in use: " + request.getSlotNumber());
            }
        }
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            boolean couponExists = id == null
                    ? bannerPromotionRepository.existsByCouponCodeIgnoreCaseAndDeletedFalse(request.getCouponCode())
                    : bannerPromotionRepository.existsByCouponCodeIgnoreCaseAndDeletedFalseAndIdNot(request.getCouponCode(), id);
            if (couponExists) {
                throw new BannerValidationException("Coupon code already exists");
            }
        }
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "priority", "slotNumber", "startDate", "endDate", "createdAt", "updatedAt", "title" -> sortBy;
            default -> "createdAt";
        };
    }

    private Long currentUserId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }
}

