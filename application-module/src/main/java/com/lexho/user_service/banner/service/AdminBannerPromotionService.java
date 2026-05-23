package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AdminBannerPromotionService.class);

    private final BannerPromotionRepository bannerPromotionRepository;
    private final BannerPromotionMapper bannerPromotionMapper;
    private final SecurityUtils securityUtils;
    private final MediaUrlResolver mediaUrlResolver;

    public AdminBannerPromotionService(BannerPromotionRepository bannerPromotionRepository,
                                       BannerPromotionMapper bannerPromotionMapper,
                                       SecurityUtils securityUtils,
                                       MediaUrlResolver mediaUrlResolver) {
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.bannerPromotionMapper = bannerPromotionMapper;
        this.securityUtils = securityUtils;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse create(BannerUpsertRequest request) {
        log.info("Create banner payload={}", request);
        normalizeBannerImagePath(request);
        validateRequest(request, null, null);
        BannerPromotion banner = new BannerPromotion();
        bannerPromotionMapper.apply(banner, request);
        banner.setBannerImageUrl(mediaUrlResolver.toStorablePath(request.getBannerImageUrl()));
        log.info("Creating banner: title={}, incomingImage={}, resolvedImage={}",
                request.getTitle(), request.getBannerImageUrl(), banner.getBannerImageUrl());
        banner.setDeleted(false);
        banner.setCreatedBy(currentUserId());
        banner.setUpdatedBy(currentUserId());
        BannerPromotion saved = bannerPromotionRepository.save(banner);
        return toPublicResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse update(Long id, BannerUpsertRequest request) {
        log.info("Update banner id={}, payload={}", id, request);
        normalizeBannerImagePath(request);
        BannerPromotion banner = getBanner(id);
        log.info("Updating banner id={}: payload title={}, payloadImage={}", id, request.getTitle(), request.getBannerImageUrl());
        validateRequest(request, id, banner);
        bannerPromotionMapper.apply(banner, request);
        banner.setBannerImageUrl(mediaUrlResolver.toStorablePath(request.getBannerImageUrl()));
        log.info("Resolved banner image path for id={}: {}", id, banner.getBannerImageUrl());
        banner.setUpdatedBy(currentUserId());
        BannerPromotion saved = bannerPromotionRepository.save(banner);
        return toPublicResponse(saved);
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
        return toPublicResponse(getBanner(id));
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
        List<BannerPromotionResponse> content = result.getContent().stream().map(this::toPublicResponse).toList();
        return new BannerPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse activate(Long id) {
        BannerPromotion banner = getBanner(id);
        if (banner.getStatus() == BannerStatus.EXPIRED) {
            throw new BannerValidationException("Expired banner cannot be activated");
        }
        if (bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalseAndIdNot(
                BannerStatus.ACTIVE, banner.getPlacement(), banner.getSlotNumber(), id)) {
            throw new BannerValidationException("Active banner slot already in use for placement "
                    + banner.getPlacement() + ": " + banner.getSlotNumber());
        }
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setUpdatedBy(currentUserId());
        return toPublicResponse(bannerPromotionRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse deactivate(Long id) {
        BannerPromotion banner = getBanner(id);
        banner.setStatus(BannerStatus.INACTIVE);
        banner.setUpdatedBy(currentUserId());
        return toPublicResponse(bannerPromotionRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public BannerPromotionResponse moveToDraft(Long id) {
        BannerPromotion banner = getBanner(id);
        banner.setStatus(BannerStatus.DRAFT);
        banner.setUpdatedBy(currentUserId());
        return toPublicResponse(bannerPromotionRepository.save(banner));
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

    private void validateRequest(BannerUpsertRequest request, Long id, BannerPromotion existingBanner) {
        if (request.getPlatformType() == null) {
            request.setPlatformType(BannerPlatformType.ALL);
        }
        if (request.getNewUserOnly() == null) {
            request.setNewUserOnly(false);
        }
        if (request.getPremiumUserOnly() == null) {
            request.setPremiumUserOnly(false);
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BannerValidationException("Title is required");
        }
        String requestedImageUrl = request.getBannerImageUrl() == null ? "" : request.getBannerImageUrl().trim();
        String existingImageUrl = existingBanner == null || existingBanner.getBannerImageUrl() == null
                ? "" : existingBanner.getBannerImageUrl().trim();
        if (requestedImageUrl.isBlank() && existingImageUrl.isBlank()) {
            throw new BannerValidationException("Banner image is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BannerValidationException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BannerValidationException("End date must be greater than or equal to start date");
        }
        if (request.getStatus() == BannerStatus.ACTIVE) {
            boolean slotExists = id == null
                    ? bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalse(
                    BannerStatus.ACTIVE, request.getPlacement(), request.getSlotNumber())
                    : bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalseAndIdNot(
                    BannerStatus.ACTIVE, request.getPlacement(), request.getSlotNumber(), id);
            if (slotExists) {
                throw new BannerValidationException("Active banner slot already in use for placement "
                        + request.getPlacement() + ": " + request.getSlotNumber());
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

    private BannerPromotionResponse toPublicResponse(BannerPromotion banner) {
        BannerPromotionResponse response = bannerPromotionMapper.toResponse(banner);
        response.setBannerImageUrl(mediaUrlResolver.toPublicUrl(response.getBannerImageUrl()));
        return response;
    }

    private void normalizeBannerImagePath(BannerUpsertRequest request) {
        if (request == null) {
            return;
        }
        String rawValue = request.getBannerImageUrl();
        String storablePath = mediaUrlResolver.toStorablePath(rawValue);
        request.setBannerImageUrl(storablePath);
    }
}
